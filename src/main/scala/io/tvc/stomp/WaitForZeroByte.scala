package io.tvc.stomp

import akka.stream.stage.GraphStageLogic.{EagerTerminateOutput, TotallyIgnorantInput}
import akka.stream.stage.{GraphStage, GraphStageLogic}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.util.ByteString

/**
  * A graph stage that will accumulate bytes until it hits a null byte,
  * at which point it will emit everything it got so far & start again
  */
object WaitForZeroByte extends GraphStage[FlowShape[ByteString, ByteString]] {

  private val in: Inlet[ByteString] = Inlet[ByteString]("Incoming bytes")
  private val out: Outlet[ByteString] = Outlet[ByteString]("Outgoing bytes")
  override def shape: FlowShape[ByteString, ByteString] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    var accumulatedBytes: ByteString = ByteString.empty
    setHandler(out, EagerTerminateOutput)
    setHandler(in, TotallyIgnorantInput)

    private def readMore(): Unit = //noinspection ConvertibleToMethodValue
      read(in, handleNew(_), () => emit(out, accumulatedBytes, () => completeStage))

    private def handleNew(newBytes: ByteString) = {
      val bytesSoFar = accumulatedBytes ++ newBytes
      val (toEmit, toAccumulate) = bytesSoFar.splitAt(bytesSoFar.indexOf(Char.MinValue))
      accumulatedBytes = toAccumulate.filterNot(_ == Char.MinValue)
      if (toEmit.nonEmpty) {
        emit(out, toEmit, () => readMore())
      } else {
        readMore()
      }
    }

    override def preStart(): Unit =
      readMore()
  }
}
