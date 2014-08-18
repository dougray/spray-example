package com.fidesmo.examples.spray


import java.util.UUID
import akka.actor.Actor
import spray.httpx._
import spray.http._

trait DeliveryActor extends ResponseTransformation with Actor {
  import SprayJsonSupport._
  import Models._

  val unmarshalOperation = unmarshal[OperationResponse]
  def waitForOperationId(nextState: UUID => Receive): Receive = {
    case response: HttpResponse =>
      // Get the operation id from the response
      val operationId = unmarshalOperation(response).operationId
      // Use the operation id
      context.become(nextState(operationId))
  }
}
