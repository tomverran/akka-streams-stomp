package io.tvc.stomp

import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, Matchers}

import scala.concurrent.Future

class StompDecoderTest extends FreeSpec with Matchers with AkkaTest with ScalaFutures {

  def decode(msg: ByteString): Future[Seq[StompMessage[ByteString]]] =
    Source(msg.toList).via(StompDecoder).runWith(Sink.seq)
  
  "STOMP message decoder" - {

    "Should be able to decode messages with only a verb" in {
      val msg = StompSource.stompFrame(verb = "VERB", headers = List.empty, body = None)
      whenReady(decode(msg))(_.head shouldEqual StompMessage(verb = "VERB", headers = Map.empty, body = None))
    }

    "Should ignore any malformed headers" in {
      val msg = StompSource.toUtf8("VERB\nfoobar\n\n") :+ ZERO_OCTET
      whenReady(decode(msg))(_.head shouldEqual StompMessage(verb = "VERB", headers = Map.empty, body = None))
    }

    "Should create byteStrings from the body of the message" in {
      val body = StompSource.toUtf8("Why hello there, isn't the weather dreadful today?")
      val msg = StompSource.stompFrame(verb = "VERB", headers = List.empty, body = Some(body))
      whenReady(decode(msg))(_.head shouldEqual StompMessage(verb = "VERB", headers = Map.empty, body = Some(body)))
    }

    "Should work when both headers and a body exist" in {
      val msg = StompSource.stompFrame(verb = "VERB", headers = List("a" -> "b", "c" -> "d"), body = Some(StompSource.toUtf8("foo")))
      whenReady(decode(msg))(_.head shouldEqual StompMessage(verb = "VERB", headers = Map("a" -> "b", "c" -> "d"), body = Some(ByteString("foo"))))
    }

    "Should work with binary content containing null octets if a content-length is supplied" in {
      val binaryBody = ByteString('a'.toByte, 'b'.toByte, ZERO_OCTET, 'c'.toByte, 'd'.toByte, ZERO_OCTET, 'e'.toByte)
      val msg = StompSource.stompFrame(verb = "VERB", headers = List("content-length" -> s"${binaryBody.length}"), body = Some(binaryBody))
      whenReady(decode(msg).map(_.head.body))(_ shouldEqual Some(binaryBody))
    }

    "Should work with messages containing trailing newlines" in {
      val frame1 = StompSource.stompFrame("foo", List.empty, Some(ByteString("bar"))) ++ ByteString('\n', '\n', '\n')
      val frame2 = StompSource.stompFrame("bar", List.empty, Some(ByteString("foo")))
      whenReady(decode(frame1 ++ frame2)) { results =>
        results shouldEqual Vector(
          StompMessage[ByteString](verb = "foo", headers = Map.empty, body = Some(ByteString("bar"))),
          StompMessage[ByteString](verb = "bar", headers = Map.empty, body = Some(ByteString("foo")))
        )
      }
    }

    "Should substitute STOMP escape characters for their real counterparts" in {
      pending
    }
  }
}
