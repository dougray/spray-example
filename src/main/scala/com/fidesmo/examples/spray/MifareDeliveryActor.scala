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

class MifareDeliveryActor(val sessionId: UUID) extends Actor with DeliveryActor with RequestBuilding {
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
  val callbackUrlRead = baseUrl.withPath(baseUrl.path / "read" / sessionId.toString)


  // Adds authentication and session id headers
  val headers = addHeader("app_id", appId) ~> addHeader("app_key", appKey) ~>
     addHeader("sessionId", sessionId.toString)

  // Header for providing the callback url to the transceive endpoint
  val callbackHeader = addHeader("callbackUrl", callbackUrl.toString)

  // Post message to get a mifare card
  val getCard = Post(FidesmoMifareGet, MifareGetCard(16)) ~> headers ~>
    addHeader("callbackUrl", callbackUrlGetCard.toString)

  def hex(s: String): Array[Byte] = Hex.decodeHex(s.toCharArray)

  val DefaultAccess = hex("F87F0000")

  val TransportKeys = KeyPair(hex("FFFFFFFFFFFF"), hex("FFFFFFFFFFFF"))

  val InitializePayload = InitializeRequest((0 until 16).map(Trailer(_, TransportKeys, DefaultAccess)), true)

  val initialize = Put(FidesmoMifareInitialize, InitializePayload) ~> headers ~>
    callbackHeader

  val blocks = (0 until 3).map(BlockIndex(1, _))

  def writePayload(checksum: String) =
    WriteRequest(blocks = blocks.map { block =>
      Block(block.sector, block.block, Array((block.sector * 4 + block.block).toByte) ++
        hex("000000000000000000000000000000"))
    }, checksum = checksum)

  def write(checksum: String) = Put(FidesmoMifareWrite, writePayload(checksum)) ~> headers ~> callbackHeader

  val read = Put(FidesmoMifareRead, ReadRequest(blocks = blocks)) ~> headers ~>
    addHeader("callbackUrl", callbackUrlRead.toString)

  // Post message to signal successful service delivery
  val Success = Post(FidesmoServiceComplete, ServiceStatus(true, "Successfully delivered test service!")) ~> headers

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
      IO(Http) ! getCard
      // Wait for the operation id from Fidesmo
      context.become(waitForOperationId(waitForCard))
  }

  def waitForCard(operationId: UUID): Receive = {
    case GetCardResponse(opId, StatusCodes.OK, Some(uid), Some(true)) =>
      /* New card, need to initialize it with keys */
      IO(Http) ! initialize
      context.become(waitForOperationId(waitForInitialization))
    case GetCardResponse(opId, StatusCodes.OK, Some(uid), Some(false)) =>
      /* Existing card, no need to initialize it */
      IO(Http) ! read
      context.become(waitForOperationId(waitForRead))
    case _ =>
      /* Everything else is an error */
      complete(Failure)
  }

  def waitForInitialization(operationId: UUID): Receive = {
    case GenericResponse(opId, StatusCodes.OK) =>
      /* Got it! */
      IO(Http) ! read
      context.become(waitForOperationId(waitForRead))
    case _ =>
      /* Everything else is an error */
      complete(Failure)
  }

  def waitForRead(operationId: UUID): Receive = {
    case ReadResponse(opId, StatusCodes.OK, blocks, checksum) =>
      /* Read blocks and got checksum */
      IO(Http) ! write(checksum)
      context.become(waitForOperationId(waitForWrite))
    case _ =>
      /* Everything else is an error */
      complete(Failure)
  }

  def waitForWrite(operationId: UUID): Receive = {
    case GenericResponse(opId, StatusCodes.OK) =>
      /* Write completed! */
      complete(Success)
    case _ =>
      /* Everything else is an error */
      complete(Failure)
  }

}

object MifareDeliveryActor {
  def props(sessionId: UUID) = Props(classOf[MifareDeliveryActor], sessionId)
}
