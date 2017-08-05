package io.tvc.stomp

import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, Matchers}

import scala.concurrent.Future

class StompDecoderTest extends FreeSpec with Matchers with AkkaTest with ScalaFutures {

  def decode(msg: ByteString): Future[StompMessage[ByteString]] =
    Source(msg.toList).via(StompDecoder).runWith(Sink.head)
  
  "STOMP message decoder" - {

    "Should be able to decode messages with only a verb" in {
      val msg = StompSource.stompFrame(verb = "VERB", headers = List.empty, body = None)
      whenReady(decode(msg))(_ shouldEqual StompMessage(verb = "VERB", headers = Map.empty, body = None))
    }

    "Should ignore any malformed headers" in {
      val msg = StompSource.toUtf8("VERB\nfoobar\n\n") :+ ZERO_OCTET
      whenReady(decode(msg))(_ shouldEqual StompMessage(verb = "VERB", headers = Map.empty, body = None))
    }

    "Should create byteStrings from the body of the message" in {
      val body = "Why hello there, isn't the weather dreadful today?"
      val msg = StompSource.stompFrame(verb = "VERB", headers = List.empty, body = Some(body))
      whenReady(decode(msg))(_ shouldEqual StompMessage(verb = "VERB", headers = Map.empty, body = Some(ByteString(body))))
    }

    "Should work when both headers and a body exist" in {
      val msg = StompSource.stompFrame(verb = "VERB", headers = List("a" -> "b", "c" -> "d"), body = Some("foo"))
      whenReady(decode(msg))(_ shouldEqual StompMessage(verb = "VERB", headers = Map("a" -> "b", "c" -> "d"), body = Some(ByteString("foo"))))
    }

    "Should substitute STOMP escape characters for their real counterparts" in {
      pending
    }
  }
}
