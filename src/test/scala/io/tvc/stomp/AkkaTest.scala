package io.tvc.stomp

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.concurrent.ExecutionContextExecutor

trait AkkaTest extends BeforeAndAfterAll { self: Suite =>
  implicit val as: ActorSystem = ActorSystem()
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = mat.executionContext
  override def afterAll(): Unit = as.terminate()
}
