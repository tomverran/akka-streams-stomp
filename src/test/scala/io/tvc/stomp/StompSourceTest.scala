package io.tvc.stomp

import org.scalatest.{FlatSpec, Matchers}

class StompSourceTest extends FlatSpec with Matchers {


  "STOMP message decoder" - {

    "Should be able to decode empty heartbeat STOMP frames" in {
      StompSource.decode(StompSource.heartbeat) shouldEqual StompMessage()


    }


  }



}
