package com.fidesmo.examples.spray

import java.util.UUID

import org.apache.commons.codec.binary.Hex
import spray.http.{ StatusCode, StatusCodes }
import spray.json._

object Models extends DefaultJsonProtocol {
  case class ServiceDeliveryRequest(sessionId: UUID, serviceId: String, description: ServiceDescription)
  case class ServicePrice(total: BigDecimal)
  case class ServiceRequirements(
    fixedUid: Option[String],
    javaCard: Option[String],
    mifare: Option[String])

  case class ServiceDescription(title: String, price: Option[ServicePrice] = None,
    confirmationRequired: Option[Boolean] = None, requirements: Option[ServiceRequirements] = None)
  case class OperationResponse(operationId: UUID)
  case class ServiceStatus(success: Boolean, message: String)

  case class Transceive(commands: Seq[Array[Byte]])
  case class TransceiveResponse(operationId: UUID, statusCode: StatusCode, responses: Option[Seq[Array[Byte]]])

  case class GetCardResponse(operationId: UUID, statusCode: StatusCode, uid: Option[Array[Byte]],
    newCard: Option[Boolean])
  case class GenericResponse(operationId: UUID, statusCode: StatusCode)
  case class KeyPair(keyA: Array[Byte], keyB: Array[Byte])
  case class Trailer(sector: Int, keyPair: KeyPair, accessBits: Array[Byte])
  case class InitializeRequest(trailers: Seq[Trailer], overwriteInvalidAccessBits: Boolean)
  case class BlockIndex(sector: Int, block: Int)
  case class Block(sector: Int, block: Int, data: Array[Byte])
  case class WriteRequest(blocks: Seq[Block], checksum: String)
  case class ReadRequest(blocks: Seq[BlockIndex])
  case class ReadResponse(operationId: UUID, statusCode: StatusCode, blocks: Seq[Block], checksum: String)

  case class CcmInstallRequest(executableLoadFile: String, executableModule: String,
    application: String)

  implicit object StatusCodeFormat extends JsonFormat[StatusCode] {
    val errorMsg = "Integer status code expected"
    def write(statusCode: StatusCode) = JsNumber(statusCode.intValue)
    def read(value: JsValue) = value match {
      case JsNumber(statusCode) => {
        if (statusCode.isValidInt)
          StatusCode.int2StatusCode(statusCode.intValue)
        else
          deserializationError(errorMsg)
      }
      case _ => deserializationError(errorMsg)
    }
  }

  implicit object ByteArrayFormat extends JsonFormat[Array[Byte]] {
    def write(byteArray: Array[Byte]) = JsString(Hex.encodeHexString(byteArray).toUpperCase)
    def read(value: JsValue): Array[Byte] = {
      value match {
        case JsString(byteString) => Hex.decodeHex(byteString.toCharArray)
        case _                    => throw new DeserializationException("Expected hexadecimal byte string")
      }
    }
  }

  implicit object UUIDFormat extends JsonFormat[UUID] {
    def write(uuid: UUID) = JsString(uuid.toString)
    def read(value: JsValue) = {
      value match {
        case JsString(uuid) => UUID.fromString(uuid)
        case _              => throw new DeserializationException("Expected hexadecimal UUID string")
      }
    }
  }

  implicit val servicePriceFormat = jsonFormat1(ServicePrice)
  implicit val serviceRequirementsFormat = jsonFormat3(ServiceRequirements)
  implicit val serviceDescriptionFormat = jsonFormat4(ServiceDescription)
  implicit val serviceDeliveryRequestFormat = jsonFormat3(ServiceDeliveryRequest)
  implicit val operationResponseFormat = jsonFormat1(OperationResponse)
  implicit val serviceStatusFormat = jsonFormat2(ServiceStatus)

  implicit val transceiveFormat = jsonFormat1(Transceive)
  implicit val transceiveResponseFormat = jsonFormat3(TransceiveResponse)

  implicit val getCardResponseFormat = jsonFormat4(GetCardResponse)
  implicit val genericResponseFormat = jsonFormat2(GenericResponse)
  implicit val keyPairFormat = jsonFormat2(KeyPair)
  implicit val trailerFormat = jsonFormat3(Trailer)
  implicit val initializeRequestFormat = jsonFormat2(InitializeRequest)
  implicit val blockIndexFormat = jsonFormat2(BlockIndex)
  implicit val blockFormat = jsonFormat3(Block)
  implicit val writeRequestFormat = jsonFormat2(WriteRequest)
  implicit val readRequestFormat = jsonFormat1(ReadRequest)
  implicit val readResponseFormat = jsonFormat4(ReadResponse)
  implicit val ccmInstallRequestFormat = jsonFormat3(CcmInstallRequest)

}
