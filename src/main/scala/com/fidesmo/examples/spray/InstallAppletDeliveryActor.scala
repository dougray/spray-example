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

class InstallAppletDeliveryActor(val sessionId: UUID) extends Actor with DeliveryActor with RequestBuilding {
  import WebServiceActor.Start
  import spray.httpx.SprayJsonSupport._
  import Models._

  implicit val system = context.system
  implicit val executionContext = context.dispatcher

  // Fidesmo api endpoints for managing MIFARE
  val FidesmoCcmInstall = "https://api.fidesmo.com/ccm/install"

  // Fidesmo api endpoint for completing the service delivery
  val FidesmoServiceComplete = Uri("https://api.fidesmo.com/service/completed")

  // Store api credentials in config
  val config = system.settings.config.getConfig("delivery-actor")
  val appId = config.getString("fidesmo-app-id")
  val appKey = config.getString("fidesmo-app-key")

  // As well as the base of callback url
  val baseUrl = Uri(config.getString("callback-url-base"))

  // Callback for result
  val callbackUrl = baseUrl.withPath(baseUrl.path / "generic" / sessionId.toString)

  // Adds authentication and session id headers
  val headers = addHeader("app_id", appId) ~> addHeader("app_key", appKey) ~>
     addHeader("sessionId", sessionId.toString) ~> addHeader("callbackUrl", callbackUrl.toString)

  val elfAid = "A00000061700E26B8F1201"
  val emAid = "A00000061700E26B8F120101"
  val instanceAid = "A00000061700E26B8F120101"
  val install = Put(FidesmoCcmInstall, CcmInstallRequest(elfAid, emAid, instanceAid)) ~> headers

  // Post message to signal successful service delivery
  val Success = Post(FidesmoServiceComplete, ServiceStatus(true, "Successfully delivered test service!")) ~> headers

  // Post message to signal failed service delivery
  val Failure = Post(FidesmoServiceComplete, ServiceStatus(false, "Failed delivering test service.")) ~> headers

  def complete(message: HttpRequest) = {
    IO(Http) ! message
    context.stop(self)
  }

  def receive = {
    case Start =>
      // Send the first part of the delivery (here represented by a SELECT)
      IO(Http) ! install
      // Wait for the operation id from Fidesmo
      context.become(waitForOperationId(waitForInstall))
  }

  def waitForInstall(OperationId: UUID): Receive = {
    case GenericResponse(OperationId, StatusCodes.OK) =>
      /* Success! */
      complete(Success)
    case _ =>
      /* Everything else is an error */
      complete(Failure)
  }
}

object InstallAppletDeliveryActor {
  def props(sessionId: UUID) = Props(classOf[InstallAppletDeliveryActor], sessionId)
}
