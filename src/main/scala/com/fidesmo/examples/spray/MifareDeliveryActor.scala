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

class MifareDeliveryActor(val sessionId: UUID) extends Actor with RequestBuilding with ResponseTransformation {
  import WebServiceActor.Start
  import spray.httpx.SprayJsonSupport._
  import Models._

  implicit val system = context.system
  implicit val executionContext = context.dispatcher

  // Fidesmo api endpoints for managing MIFARE
  val FidesmoMifareBase = "https://api.fidesmo.com/mifare/"
  val FidesmoMifareGet = Uri(s"${FidesmoMifareBase}get")
  val FidesmoMifareInitialize = Uri(s"${FidesmoMifareBase}initialize")
  val FidesmoMifareRead = Uri(s"${FidesmoMifareBase}read")
  val FidesmoMifareWrite = Uri(s"${FidesmoMifareBase}write")

  // Fidesmo api endpoint for completing the service delivery
  val FidesmoServiceComplete = Uri("https://api.fidesmo.com/service/completed")

  // Store api credentials in config
  val config = system.settings.config.getConfig("delivery-actor")
  val appId = config.getString("fidesmo-app-id")
  val appKey = config.getString("fidesmo-app-key")
  // As well as the base of callback url
  val baseUrl = Uri(config.getString("callback-url-base"))

  // Create an individual callback URL for each session
  val callbackUrl = baseUrl.withPath(baseUrl.path / "generic" / sessionId.toString)
  val callbackUrlGetCard = baseUrl.withPath(baseUrl.path / "getCard" / sessionId.toString)


  // Adds authentication and session id headers
  val headers = addHeader("app_id", appId) ~> addHeader("app_key", appKey) ~>
     addHeader("sessionId", sessionId.toString)

  // Header for providing the callback url to the transceive endpoint
  val callbackHeader = addHeader("callbackUrl", callbackUrl.toString)

  // Post message to get a mifare card
  val GetCard = Put(FidesmoMifareGet) ~> headers ~>
    addHeader("callbackUrl", callbackUrlGetCard.toString)

  def hex(s: String): Array[Byte] = Hex.decodeHex(s.toCharArray)

  val DefaultAccess = hex("F87F0000")

  val TransportKeys = KeyPair(hex("FFFFFFFFFFFF"), hex("FFFFFFFFFFFF"))

  val InitializePayload = InitializeRequest((0 until 16).map(Trailer(_, TransportKeys, DefaultAccess)))

  val Initialize = Put(FidesmoMifareInitialize, InitializePayload) ~> headers ~>
    callbackHeader

  def writePayload(checksum: String) =
    WriteRequest(blocks = (1 until 16).flatMap { sector =>
      (0 until 3).map { block =>
        Block(sector, block, Array((sector * block).toByte) ++ hex("000000000000000000000000000000"))
      }
    }, checksum = checksum)

  def write(checksum: String) = Put(FidesmoMifareWrite, writePayload(checksum)) ~> headers ~> callbackHeader

  // Post message to signal successful service delivery
  val Success = Post(FidesmoServiceComplete, ServiceStatus(true, "Successfully delivered test service!")) ~> headers

  // Post message to signal failed service delivery
  val Failure = Post(FidesmoServiceComplete, ServiceStatus(false, "Failed delivering test service.")) ~> headers

  val unmarshalOperation = unmarshal[OperationResponse]
  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  def complete(message: HttpRequest) = {
    IO(Http) ! message
    context.stop(self)
  }

  def receive = {
    case Start =>
      // Send the first part of the delivery (here represented by a SELECT)
      IO(Http) ! GetCard
      // Wait for the operation id from Fidesmo
      context.become(waitForOperationId(waitForCard))
  }

  def waitForCard(operationId: UUID): Receive = {
    case GetCardResponse(opId, StatusCodes.OK, Some(uid), Some(true), Some(checksum)) =>
      println("Initialize")
      /* New card, need to initialize it with keys */
      IO(Http) ! Initialize
      context.become(waitForOperationId(waitForInitialization(checksum)))
    case GetCardResponse(opId, StatusCodes.OK, Some(uid), Some(false), Some(checksum)) =>
      /* Existing card, no need to initialize it */
      println("No initialize")
      IO(Http) ! write(checksum)
      context.become(waitForOperationId(waitForWrite))
    case a =>
      println("Wait card")
      println(a)
      /* Everything else is an error */
      complete(Failure)
  }

  def waitForInitialization(checksum: String)(operationId: UUID): Receive = {
    case GenericResponse(opId, StatusCodes.OK) =>
      /* Got it! */
      IO(Http) ! write(checksum)
      context.become(waitForOperationId(waitForWrite))
    case a =>
      println("Wait init")
      println(a)
      /* Everything else is an error */
      complete(Failure)
  }

  def waitForWrite(operationId: UUID): Receive = {
    case GenericResponse(opId, StatusCodes.OK) =>
      /* Write completed! */
      complete(Success)
    case a =>
      println("Wait write")
      println(a)
      /* Everything else is an error */
      complete(Failure)
  }

  def waitForOperationId(nextState: UUID => Receive): Receive = {
    case response: HttpResponse =>
      // Get the operation id from the response
      val operationId = unmarshalOperation(response).operationId
      // Use the operation id
      context.become(nextState(operationId))
  }
}

object MifareDeliveryActor {
  def props(sessionId: UUID) = Props(classOf[MifareDeliveryActor], sessionId)
}
