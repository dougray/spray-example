package com.fidesmo.examples.spray

import java.util.UUID

import akka.actor.{ Actor, Props }
import akka.io.IO

import spray.can.Http
import spray.http._
import spray.httpx._

class FailDeliveryActor(val sessionId: UUID) extends Actor with RequestBuilding {
  import WebServiceActor.Start
  import spray.httpx.SprayJsonSupport._
  import Models._

  implicit val system = context.system

  // Store api credentials in config
  val config = system.settings.config.getConfig("delivery-actor")
  val appId = config.getString("fidesmo-app-id")
  val appKey = config.getString("fidesmo-app-key")

  // Header for providing the callback url to the transceive endpointx
  val headers = addHeader("app_id", appId) ~> addHeader("app_key", appKey) ~>
     addHeader("sessionId", sessionId.toString)

  // Fidesmo api endpoint for completing the service delivery
  val FidesmoServiceCompleteApi = Uri("https://api.fidesmo.com/service/completed")

  val failure = Post(FidesmoServiceCompleteApi, ServiceStatus(false, "Failed delivering test service.")) ~> headers

  def receive = {
    case Start =>
      // Do nothing and signal service delivery failure
      IO(Http) ! failure
  }
}

object FailDeliveryActor {
  def props(sessionId: UUID) = Props(classOf[FailDeliveryActor], sessionId)
}
