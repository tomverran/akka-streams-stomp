package io.tvc.stomp

import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, Matchers}

class WaitForZeroByteTest extends FreeSpec with Matchers with ScalaFutures with AkkaTest {

  "Wait for zero byte" - {

    "should emit everything it got if there were no zero bytes" in {
      val result = Source(List("foo", "bar", "baz").map(ByteString(_))).via(WaitForZeroByte).runWith(Sink.seq)
      whenReady(result)(_ shouldEqual Vector(ByteString("foobarbaz")))
    }

    "should split things on any zero bytes it gets" in {
      val result = Source(
        List(
          ByteString('a'),
          ByteString("bcdefg"),
          ByteString(Char.MinValue),
          ByteString("foo")
        )
      ).via(WaitForZeroByte)
        .runWith(Sink.seq)

      whenReady(result) { r =>
        r shouldEqual Vector(ByteString("abcdefg"), ByteString("foo"))
      }
    }
  }
}
