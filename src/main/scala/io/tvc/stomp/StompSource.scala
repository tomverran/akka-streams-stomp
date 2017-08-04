package io.tvc.stomp

import java.net.InetSocketAddress
import java.nio.charset.{Charset, CodingErrorAction}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Concat, Source, Tcp}
import akka.util.ByteString

import scala.concurrent.duration._
import scala.util.Try

object StompSource {

  /**
    * Create a stomp frame, ready to send to the external server
    */
  private[stomp] def stompFrame(
    verb: String,
    headers: List[(String, String)],
    body: Option[String]
  ): ByteString =
    ByteString(
      s"""
       |$verb
       |${headers.map { case (k, v) => s"$k:$v\n" }.mkString }
       |${body.mkString}${Char.MinValue}""".stripMargin.stripPrefix("\n")
    )

  /**
    * Create a CONNECT stomp frame, with hard coded heart beat and version rules
    */
  private[stomp] def connect(host: InetSocketAddress, credentials: Option[Credentials]) =
    stompFrame(
      verb = "CONNECT",
      headers = List(
        "host" -> host.getHostName,
        "heart-beat" -> "1000,1000",
        "accept-version" -> "1.0,1.1,1.2"
      ) ++ credentials.toList.flatMap { creds => List(
        "login" -> creds.login,
        "passcode" -> creds.passcode
      )},
      body = None
    )

  /**
    * Create a SUBSCRIBE stomp frame,
    * hard codes the ID for now so you can only subscribe to one place
    * and also hard codes ack to auto as we have no feedback loop in place
    */
  private[stomp] def subscribe(queueName: QueueName): ByteString =
    stompFrame(
      verb = "SUBSCRIBE",
      headers = List("id" -> "0", "ack" -> "auto", "destination" -> queueName.value),
      body = None
    )

  /**
    * An empty stomp frame we hit the server with every 1 second
    * to prevent the stream from completing too early
    * annoyingly there is no NOOP verb...
    */
  private[stomp] val heartbeat: ByteString =
    stompFrame(verb = "", headers = List.empty, body = None)


  /**
    * Given a stomp message encoded into a byte string,
    * decode it to split out the headers and the body
    */
  private[stomp] def decode(bs: ByteString): StompMessage[ByteString] = {
    val (verb, rest) = bs.span(('A' to 'Z').map(_.toByte).contains)
    val (rawHeaders, body) = rest.dropRight(1).splitAt(rest.indexOfSlice(List('\n','\n')))

    val headers: List[(String, String)] = for {
      headerLine <- rawHeaders.decodeString("UTF-8").split('\n').toList if headerLine.contains(':')
      (k, v) = headerLine.span(_ != ':')
    } yield k -> v.drop(1)

   StompMessage(
      verb = verb.decodeString("UTF-8"),
      headers = headers.toMap,
      body = Some(body.drop(2)).filter(_.nonEmpty)
    )
  }


  /**
    * Create a source that is able to read STOMP messages from the given host & queue
    * The stomp server must support heartbeats.
    */
  def apply(
    queue: QueueName,
    host: InetSocketAddress,
    credentials: Option[Credentials]
  )(
    implicit
    as: ActorSystem,
    mat: ActorMaterializer
  ): Source[StompMessage[ByteString], NotUsed] = {

    val frames = List(
      connect(host, credentials),
      subscribe(queue)
    )

    Source.combine(Source(frames), Source.repeat(heartbeat))(Concat.apply)
      .throttle(1, 500.milliseconds, 1, ThrottleMode.shaping)
      .via(Tcp().outgoingConnection(remoteAddress = host))
      .via(WaitForZeroByte)
      .map(decode)
  }
}
