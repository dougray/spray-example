package com.fidesmo.examples.spray

import java.util.UUID

import org.apache.commons.codec.binary.Hex
import spray.http.{ StatusCode, StatusCodes }
import spray.json._

object Models extends DefaultJsonProtocol {
  case class ServiceDeliveryRequest(sessionId: UUID, serviceId: String)
  case class ServiceDescription(title: String)
  case class OperationResponse(operationId: UUID)
  case class ServiceStatus(success: Boolean, message: String)

  case class Transceive(commands: Seq[Array[Byte]])
  case class TransceiveResponse(operationId: UUID, statusCode: StatusCode, responses: Option[Seq[Array[Byte]]])

  case class GetCardResponse(operationId: UUID, statusCode: StatusCode, uid: Option[Array[Byte]],
    newCard: Option[Boolean], checksum: Option[String])
  case class GenericResponse(operationId: UUID, statusCode: StatusCode)
  case class KeyPair(keyA: Array[Byte], keyB: Array[Byte])
  case class Trailer(sector: Int, keyPair: KeyPair, accessBits: Array[Byte])
  case class InitializeRequest(trailers: Seq[Trailer])
  case class Block(sector: Int, block: Int, data: Array[Byte])
  case class WriteRequest(blocks: Seq[Block], checksum: String)


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

  implicit val serviceDeliveryRequestFormat = jsonFormat2(ServiceDeliveryRequest)
  implicit val serviceDescriptionFormat = jsonFormat1(ServiceDescription)
  implicit val operationResponseFormat = jsonFormat1(OperationResponse)
  implicit val serviceStatusFormat = jsonFormat2(ServiceStatus)

  implicit val transceiveFormat = jsonFormat1(Transceive)
  implicit val transceiveResponseFormat = jsonFormat3(TransceiveResponse)

  implicit val getCardResponseFormat = jsonFormat5(GetCardResponse)
  implicit val genericResponseFormat = jsonFormat2(GenericResponse)
  implicit val keyPairFormat = jsonFormat2(KeyPair)
  implicit val trailerFormat = jsonFormat3(Trailer)
  implicit val initializeRequestFormat = jsonFormat1(InitializeRequest)
  implicit val blockFormat = jsonFormat3(Block)
  implicit val writeRequestFormat = jsonFormat2(WriteRequest)

}