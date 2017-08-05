package io.tvc

package object stomp {

  // delimiter for stomp frames, used all over the place
  private [stomp] val ZERO_OCTET: Byte = Char.MinValue.toByte

  case class QueueName(value: String) extends AnyVal
  case class Credentials(login: String, passcode: String)
  case class StompMessage[A](verb: String, headers: Map[String, String], body: Option[A])


}
