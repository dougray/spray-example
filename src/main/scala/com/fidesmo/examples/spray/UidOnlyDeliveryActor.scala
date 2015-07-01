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

class UidOnlyDeliveryActor(val sessionId: UUID) extends Actor with DeliveryActor with RequestBuilding {
  import WebServiceActor.Start
  import spray.httpx.SprayJsonSupport._
  import Models._

  implicit val system = context.system
  implicit val executionContext = context.dispatcher

  // Fidesmo api endpoints for managing uid delivery
  val FidesmoUidBase = "https://api.fidesmo.com/uid/"
  val FidesmoUidRegister = Uri(s"${FidesmoUidBase}register")

  // Fidesmo api endpoint for completing the service delivery
  val FidesmoServiceComplete = Uri("https://api.fidesmo.com/service/completed")

  // Store api credentials in config
  val config = system.settings.config.getConfig("delivery-actor")
  val appId = config.getString("fidesmo-app-id")
  val appKey = config.getString("fidesmo-app-key")
  // As well as the base of callback url
  val baseUrl = Uri(config.getString("callback-url-base"))

  // Create an individual callback URL for each session
  val callbackUrlRegisterCard = baseUrl.withPath(baseUrl.path / "uidRegister" / sessionId.toString)

  // Adds authentication and session id headers
  val headers = addHeader("app_id", appId) ~> addHeader("app_key", appKey) ~>
     addHeader("sessionId", sessionId.toString)

  // Post message to get a mifare card
  val registerCard = Post(FidesmoUidRegister) ~> headers ~>
    addHeader("callbackUrl", callbackUrlRegisterCard.toString)

  // Post message to signal successful service delivery
  def success(uid: String) = Post(FidesmoServiceComplete, ServiceStatus(true, s"Successfully delivered test service! ${uid}")) ~> headers

  // Post message to signal failed service delivery
  val Failure = Post(FidesmoServiceComplete, ServiceStatus(false, "Failed delivering test service.")) ~> headers

  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  def complete(message: HttpRequest) = {
    IO(Http) ! message
    context.stop(self)
  }

  def receive = {
    case Start =>
      // Send the first part of the delivery (here represented by a SELECT)
      IO(Http) ! registerCard
      // Wait for the operation id from Fidesmo
      context.become(waitForOperationId(waitForCard))
  }

  def waitForCard(operationId: UUID): Receive = {
    case UidRegisterResponse(opId, StatusCodes.OK, Some(uid), Some(newFlag)) =>
      /* New card, need to initialize it with keys */
      if (newFlag) {
        complete(success(s"New card with id ${uid}."))
      } else {
        complete(success(s"Old card with id ${uid}."))
      }
    case _ =>
      /* Everything else is an error */
      complete(Failure)
  }
}

object UidOnlyDeliveryActor {
  def props(sessionId: UUID) = Props(classOf[UidOnlyDeliveryActor], sessionId)
}
