package io.tvc.stomp

import akka.stream.stage.GraphStageLogic.{EagerTerminateOutput, TotallyIgnorantInput}
import akka.stream.stage.{GraphStage, GraphStageLogic}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.util.ByteString

import scala.util.Try

/**
  * A graph stage that will decode stomp messages byte by byte
  * It forms a kind of state machine through mutual recursion
  */
object StompDecoder extends GraphStage[FlowShape[Byte, StompMessage[ByteString]]] {

  private val `\n` = '\n'.toByte
  private val in: Inlet[Byte] = Inlet[Byte]("Incoming bytes")
  private val out: Outlet[StompMessage[ByteString]] = Outlet[StompMessage[ByteString]]("Outgoing bytes")

  override def shape: FlowShape[Byte, StompMessage[ByteString]] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    var accumulatedBytes: ByteString = ByteString.empty
    setHandler(out, EagerTerminateOutput)
    setHandler(in, TotallyIgnorantInput)

    /**
      * Read from the inlet until we hit a particular byte
      */
    private def readUntil(c: Byte => Boolean)(finish: ByteString => Unit, soFar: ByteString = ByteString.empty): Unit =
      read(in)(b => if (c(b)) finish(soFar :+ b) else readUntil(c)(finish, soFar :+ b), () => completeStage())

    /**
      * Read a particular number of bytes from the inlet
      */
    private def readUntilN(number: Int)(finish: ByteString => Unit): Unit =
      readN(in, number)(bytes => finish(ByteString(bytes.toArray)), bytes => completeStage())

    /**
      * Decode the verb part of the stomp message,
      * when finished it will begin decoding headers
      */
    private def decodeVerb(prefix: ByteString = ByteString.empty): Unit =
      readUntil(_ == `\n`)(bs => decodeHeaders((prefix ++ bs).dropRight(1).decodeString("UTF-8")))

    /**
      * Decode the headers of the stomp message,
      * when finished it will begin decoding the body
      */
    private def decodeHeaders(verb: String, soFar: Map[String, String] = Map.empty): Unit =
      readUntil(_ == `\n`) { bs =>
        if (bs.forall(_ == `\n`)) {
          decodeBody(verb, soFar)
        } else {
          val (key, value) = bs.dropRight(1).decodeString("UTF-8").span(_ != ':')
          decodeHeaders(verb, soFar.updated(key, value.drop(1)).filter { case (k, v) => k.trim.nonEmpty && v.trim.nonEmpty })
        }
      }

    /**
      * Decode the body part of the stomp message,
      * when finished it will emit it and call decodeVerb
      */
    private def decodeBody(verb: String, headers: Map[String, String]): Unit = {
      val contentLength = headers.get("content-length").flatMap(c => Try(c.toInt + 1).toOption)
      val onComplete: ByteString => Unit = { bs =>
        val output = StompMessage(verb, headers, Some(bs.dropRight(1)).filter(_.nonEmpty))
        emit(out, output, () => readUntil(_ != '\n'.toByte)(firstLetter => decodeVerb(firstLetter.takeRight(1))))
      }
      contentLength.fold[Unit](readUntil(_ == ZERO_OCTET)(onComplete))(readUntilN(_)(onComplete))
    }

    /**
      * Kick off the decodeVerb -> decodeHeaders -> decodeBody loop
      * by first trying to decode the verb
      */
    override def preStart(): Unit =
      decodeVerb()
  }
}
