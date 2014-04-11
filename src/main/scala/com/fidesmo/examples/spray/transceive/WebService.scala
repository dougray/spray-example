package com.fidesmo.examples.spray.transceive

import akka.actor.Props
import spray.routing.HttpServiceActor
import spray.json._
import spray.http.StatusCodes

class WebServiceActor extends HttpServiceActor {
  import spray.httpx.SprayJsonSupport._
  import WebServiceActor._
  import DeliveryActor.Start
  import Models._

  val route = path("service") { // This path is for the service delivery required call
    entity(as[ServiceDeliveryRequest]) { request =>
      val sessionId = request.sessionId
      val deliveryActor = context.actorOf(DeliveryActor.props(sessionId), sessionId.toString)
      // Start the delivery actor
      deliveryActor ! Start
      complete(ServiceDescription("Test service")) // Return the service description to Fidesmo
    }
  } ~ path("delivery" / JavaUUID) { sessionId => // This path is for the transceive callback
    val deliveryActor = context.actorSelection(sessionId.toString)
    entity(as[TransceiveResponse]) { response =>
      // Send transceive response to delivery actor
      deliveryActor ! response
      complete(StatusCodes.OK)
    }
  }

  def receive = runRoute(route)

}

object WebServiceActor {
  def props(): Props = Props(classOf[WebServiceActor])
}
