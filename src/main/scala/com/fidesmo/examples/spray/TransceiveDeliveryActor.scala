package com.fidesmo.examples.spray

import java.util.UUID
import java.util.concurrent.TimeUnit

import org.apache.commons.codec.binary.Hex

import akka.actor.{ Actor, Props }
import akka.pattern.ask
import akka.io.IO
import akka.util.Timeout

import spray.can.Http
import spray.http._
import spray.httpx._

class TransceiveDeliveryActor(val sessionId: UUID) extends Actor with RequestBuilding with ResponseTransformation {
  import WebServiceActor.Start
  import spray.httpx.SprayJsonSupport._
  import Models._

  implicit val system = context.system
  implicit val executionContext = context.dispatcher

  // Fidesmo api endpoint for sending and receiving APDUs
  val FidesmoTransceiveApi = Uri("https://api.fidesmo.com/transceive")
  // Fidesmo api endpoint for completing the service delivery
  val FidesmoServiceCompleteApi = Uri("https://api.fidesmo.com/service/completed")

  // Store api credentials in config
  val config = system.settings.config.getConfig("delivery-actor")
  val appId = config.getString("fidesmo-app-id")
  val appKey = config.getString("fidesmo-app-key")
  // As well as the base of callback url
  val baseUrl = Uri(config.getString("callback-url-base"))

  // Create an individual callback URL for each session
  val callbackUrl = baseUrl.withPath(baseUrl.path / "transceive" / sessionId.toString)

  // Adds authentication and session id headers
  val headers = addHeader("app_id", appId) ~> addHeader("app_key", appKey) ~>
     addHeader("sessionId", sessionId.toString)

  // Header for providing the callback url to the transceive endpoint
  val callbackHeader = addHeader("callbackUrl", callbackUrl.toString)

  // Post message to transceive a simple select
  val transceive = Post(FidesmoTransceiveApi, Transceive(Seq(Hex.decodeHex("00A4040008A00000015100000000".toCharArray)))) ~> headers ~>
    callbackHeader

  // Post message to signal successful service delivery
  val success = Post(FidesmoServiceCompleteApi, ServiceStatus(true, "Successfully delivered test service!")) ~> headers

  // Post message to signal failed service delivery
  val failure = Post(FidesmoServiceCompleteApi, ServiceStatus(false, "Failed delivering test service.")) ~> headers

  val unmarshalOperation = unmarshal[OperationResponse]
  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  def receive = {
    case Start =>
      // Send the first part of the delivery (here represented by a SELECT)
      IO(Http) ! transceive
      // Wait for the operation id from Fidesmo
      context.become(waitForOperationId(false))
  }

  // This state awaits the acknowledgment of a transceive
  def waitForOperationId(last: Boolean): Receive = {
    case response: HttpResponse =>
      // Get the operation id from the response
      val operationId = unmarshalOperation(response).operationId
      // Use the operation id
      context.become(waitingForResponse(operationId, last))
  }

  // This state awaits the response of a transceive
  def waitingForResponse(waitingForId: UUID, last: Boolean): Receive = {
    case TransceiveResponse(_, StatusCodes.OK, Some(responses)) =>
      if(last) {
        // If it's the last we are done
        IO(Http) ! success
      } else {
        // Send another one
        IO(Http) ! transceive
        context.become(waitForOperationId(true))
      }
    case TransceiveResponse(_, _, _) =>
      // Something failed!
      IO(Http) ! failure
  }

}

object TransceiveDeliveryActor {
  def props(sessionId: UUID) = Props(classOf[TransceiveDeliveryActor], sessionId)
}
