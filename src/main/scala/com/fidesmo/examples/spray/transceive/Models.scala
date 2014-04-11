package com.fidesmo.examples.spray.transceive

import java.util.UUID

import org.apache.commons.codec.binary.Hex

import spray.json._

object Models extends DefaultJsonProtocol {
  case class ServiceDeliveryRequest(sessionId: UUID, serviceId: String)
  case class ServiceDescription(title: String)
  case class Transceive(commands: Seq[Array[Byte]])
  case class TransceiveResponse(success: Boolean, operationId: UUID, responses: Option[Seq[Array[Byte]]])
  case class OperationResponse(operationId: UUID)
  case class ServiceStatus(success: Boolean, message: String)

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
  implicit val transceiveFormat = jsonFormat1(Transceive)
  implicit val transceiveResponseFormat = jsonFormat3(TransceiveResponse)
  implicit val operationResponseFormat = jsonFormat1(OperationResponse)
  implicit val serviceStatusFormat = jsonFormat2(ServiceStatus)
}
