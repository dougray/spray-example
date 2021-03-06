package com.fidesmo.examples.spray

import akka.actor.{ Props, ActorRef }
import spray.routing.HttpServiceActor
import spray.json._
import spray.http.StatusCodes

class WebServiceActor extends HttpServiceActor {
  import spray.httpx.SprayJsonSupport._
  import WebServiceActor._
  import Models._

  private val NoRequirements = ServiceRequirements(
    fixedUid = None,
    mifare = None,
    cardIssuer = None)

  private def deliverService(actor: ActorRef) = {
    // Start the delivery actor
    actor ! Start
    complete(StatusCodes.OK)
  }

  val route = path("description" / Segment) {
    case "mifare" =>
      // Return the service description - this description is displayed on the user's phone
      complete(ServiceDescription("Test service using MIFARE Classic API", requirements = Some(NoRequirements.copy(mifare=Some("any")))))
    case "mifare-4k" =>
      // Return the service description - this description is displayed on the user's phone
      complete(ServiceDescription("Test service using MIFARE 4k Classic API", requirements = Some(NoRequirements.copy(mifare=Some("any")))))
    case "mifare-pay" =>
      // Return the service description - this description is displayed on the user's phone
      complete(ServiceDescription("Test service using MIFARE Classic API with payment", requirements = Some(NoRequirements.copy(mifare=Some("any"))), redeliveryIsFree = Some(true)))
    case "transceive" =>
      // Return the service description - this description is displayed on the user's phone
        complete(ServiceDescription("Test service using transceive API", requirements = Some(NoRequirements)))
    case "transceive-confirm" =>
      // Return the service description - this description is displayed on the user's phone
        complete(ServiceDescription("Test service using transceive API", Some(true), requirements = Some(NoRequirements)))
    case "install" =>
      // Return the service description - this description is displayed on the user's phone
      complete(ServiceDescription("Test service using ccm API", requirements = Some(NoRequirements)))
    case "fail" =>
      // Return the service description - this description is displayed on the user's phone
        complete(ServiceDescription("Test service that will fail", requirements = Some(NoRequirements)))
    case "fail-pay" =>
      // Return the service description - this description is displayed on the user's phone
      complete(ServiceDescription("Test service with payment that will fail", requirements = Some(NoRequirements)))
    case "uid-only" =>
      // Return the service description - this description is displayed on the user's phone
      complete(ServiceDescription("Test service can be delivered without nfc",
        requirements = Some(ServiceRequirements(
          fixedUid = Some("any"),
          mifare = None,
          cardIssuer = None))))
    case _ =>
      complete(StatusCodes.NotFound)
  } ~ path("service") { // This path is for the service delivery required call
    entity(as[ServiceDeliveryRequest]) {
      case ServiceDeliveryRequest(sessionId, "mifare-pay", ServiceDescription(_, _, _, _)) =>
        deliverService(context.actorOf(MifareDeliveryActor.props(sessionId, 16), sessionId.toString))
      case ServiceDeliveryRequest(sessionId, "mifare", ServiceDescription(_, _, _, _)) =>
        deliverService(context.actorOf(MifareDeliveryActor.props(sessionId, 16), sessionId.toString))
      case ServiceDeliveryRequest(sessionId, "mifare-4k", ServiceDescription(_, _, _, _)) =>
        deliverService(context.actorOf(MifareDeliveryActor.props(sessionId, 32), sessionId.toString))
      case ServiceDeliveryRequest(sessionId, "transceive", ServiceDescription(_, _, _, _)) =>
        deliverService(context.actorOf(TransceiveDeliveryActor.props(sessionId), sessionId.toString))
      case ServiceDeliveryRequest(sessionId, "transceive-confirm", ServiceDescription(_, Some(true), _, _)) =>
        deliverService(context.actorOf(TransceiveDeliveryActor.props(sessionId), sessionId.toString))
      case ServiceDeliveryRequest(sessionId, "install", ServiceDescription(_, _, _, _)) =>
        deliverService(context.actorOf(InstallAppletDeliveryActor.props(sessionId), sessionId.toString))
      case ServiceDeliveryRequest(sessionId, "fail", ServiceDescription(_, _, _, _)) =>
        deliverService(context.actorOf(FailDeliveryActor.props(sessionId), sessionId.toString))
      case ServiceDeliveryRequest(sessionId, "fail-pay", ServiceDescription(_, _, _, _)) =>
        deliverService(context.actorOf(FailDeliveryActor.props(sessionId), sessionId.toString))
      case ServiceDeliveryRequest(sessionId, "uid-only", ServiceDescription(_, _, Some(ServiceRequirements(Some("any"), None, None)), _)) =>
        deliverService(context.actorOf(UidOnlyDeliveryActor.props(sessionId), sessionId.toString))
      case _ =>
        complete(StatusCodes.NotFound)
    }
  } ~ path("delivery" / "generic" / JavaUUID) { sessionId =>
    val deliveryActor = context.actorSelection(sessionId.toString)
    entity(as[GenericResponse]) { response =>
      // Send response to delivery actor
      deliveryActor ! response
      complete(StatusCodes.OK)
    }
  } ~ path("delivery" / "getCard" / JavaUUID) { sessionId =>
    val deliveryActor = context.actorSelection(sessionId.toString)
    entity(as[GetCardResponse]) { response =>
      // Send response to delivery actor
      deliveryActor ! response
      complete(StatusCodes.OK)
    }
  } ~ path("delivery" / "read" / JavaUUID) { sessionId =>
    val deliveryActor = context.actorSelection(sessionId.toString)
    entity(as[ReadResponse]) { response =>
      // Send response to delivery actor
      deliveryActor ! response
      complete(StatusCodes.OK)
    }
  } ~ path("delivery" / "uidRegister" / JavaUUID) { sessionId =>
    val deliveryActor = context.actorSelection(sessionId.toString)
    entity(as[UidRegisterResponse]) { response =>
      // Send response to delivery actor
      deliveryActor ! response
      complete(StatusCodes.OK)
    }
  } ~ path("delivery" / "transceive" / JavaUUID) { sessionId =>
    val deliveryActor = context.actorSelection(sessionId.toString)
    entity(as[TransceiveResponse]) { response =>
      // Send response to delivery actor
      deliveryActor ! response
      complete(StatusCodes.OK)
    }
  }

  def receive = runRoute(route)

}

object WebServiceActor {
  case object Start
  def props(): Props = Props(classOf[WebServiceActor])
}
