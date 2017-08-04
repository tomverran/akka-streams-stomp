package io.tvc

package object stomp {
  case class QueueName(value: String) extends AnyVal
  case class Credentials(login: String, passcode: String)
  case class StompMessage(verb: String, headers: Map[String, String], body: Option[String])
}
