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

}
