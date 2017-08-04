package io.tvc.stomp

import akka.util.ByteString
import org.scalatest.{FreeSpec, Matchers}

class StompSourceTest extends FreeSpec with Matchers {

  "STOMP frame generator" - {

    "Should not have any characters before the verb begins or after the final 0 octet" in {
      val stompFrame = StompSource.stompFrame("FOO", List("a" -> "b"), Some("hello"))
      stompFrame.last shouldEqual Char.MinValue
      stompFrame.head shouldEqual 'F'
    }

    "Should allow blank verbs for heartbeat frames" in {
      StompSource.stompFrame("", List.empty, None) shouldEqual ByteString('\n', '\n', Char.MinValue)
    }

    "Should maintain two line breaks between the headers and body" in {
      val expected = ByteString(s"VERB\nfoo:bar\n\nbody${Char.MinValue}")
      StompSource.stompFrame("VERB", List("foo" -> "bar"), Some("body")) shouldEqual expected
    }
  }

  "STOMP message decoder" - {

    "Should be able to decode messages with only a verb" in {
      val msg = StompSource.stompFrame(verb = "VERB", headers = List.empty, body = None)
      StompSource.decode(msg) shouldEqual StompMessage(verb = "VERB", headers = Map.empty, body = None)
    }

    "Should ignore any malformed headers" in {
      val msg =
        s"""
          |VERB
          |foobar
          |
          |${Char.MinValue}""".stripMargin.stripPrefix("\n")

      StompSource.decode(ByteString(msg)) shouldEqual StompMessage(
        verb = "VERB",
        headers = Map.empty,
        body = None
      )
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
