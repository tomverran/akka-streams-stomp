package io.tvc

package object stomp {
  case class QueueName(value: String) extends AnyVal
  case class Credentials(login: String, passcode: String)
  case class StompMessage[A](verb: String, headers: Map[String, String], body: Option[A])
}
