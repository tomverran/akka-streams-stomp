package io.tvc.stomp

import akka.util.ByteString
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FreeSpec, Matchers}

class StompSourceTest extends FreeSpec with Matchers with GeneratorDrivenPropertyChecks {
  implicit override val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 100)

  "STOMP frame generator" - {

    "Should not have any characters before the verb begins or after the final 0 octet" in {
      val stompFrame = StompSource.stompFrame("FOO", List("a" -> "b"), Some("hello"))
      stompFrame.last shouldEqual ZERO_OCTET
      stompFrame.head shouldEqual 'F'
    }

    "Should allow blank verbs for heartbeat frames" in {
      StompSource.stompFrame("", List.empty, None) shouldEqual ByteString('\n', '\n', ZERO_OCTET)
    }

    "Should maintain two line breaks between the headers and body" in {
      val expected = StompSource.toUtf8(s"VERB\nfoo:bar\n\nbody") :+ ZERO_OCTET
      StompSource.stompFrame("VERB", List("foo" -> "bar"), Some("body")) shouldEqual expected
    }

    "Should UTF-8 encode strings so we never have more than one null byte per frame" in {
      forAll { s: String =>
        StompSource.stompFrame("foo", List("foo" -> "bar"), Some(s)).count(_ == ZERO_OCTET) shouldEqual 1
      }
    }
  }

  "STOMP message decoder" - {

    "Should be able to decode messages with only a verb" in {
      val msg = StompSource.stompFrame(verb = "VERB", headers = List.empty, body = None)
      StompSource.decode(msg) shouldEqual StompMessage(verb = "VERB", headers = Map.empty, body = None)
    }

    "Should ignore any malformed headers" in {
      val msg = StompSource.toUtf8("VERB\nfoobar\n\n") :+ ZERO_OCTET
      StompSource.decode(msg) shouldEqual StompMessage(verb = "VERB", headers = Map.empty, body = None)
    }

    "Should create byteStrings from the body of the message" in {
      val body = "Why hello there, isn't the weather dreadful today?"
      val msg = StompSource.stompFrame(verb = "VERB", headers = List.empty, body = Some(body))
      StompSource.decode(msg) shouldEqual StompMessage(verb = "VERB", headers = Map.empty, body = Some(ByteString(body)))
    }

    "Should work when both headers and a body exist" in {
      val msg = StompSource.stompFrame(verb = "VERB", headers = List("a" -> "b", "c" -> "d"), body = Some("foo"))
      StompSource.decode(msg) shouldEqual StompMessage(verb = "VERB", headers = Map("a" -> "b", "c" -> "d"), body = Some(ByteString("foo")))
    }

    "Should substitute STOMP escape characters for their real counterparts" in {
      pending
    }
  }
}
