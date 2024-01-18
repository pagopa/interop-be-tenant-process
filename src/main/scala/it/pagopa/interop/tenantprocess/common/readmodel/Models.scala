package it.pagopa.interop.tenantprocess.common.readmodel

import it.pagopa.interop.commons.utils.SprayCommonFormats.uuidFormat

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import java.util.UUID

final case class CertifiedAttribute(id: UUID, name: String, attributeId: UUID, attributeName: String)

object CertifiedAttribute {
  implicit val format: RootJsonFormat[CertifiedAttribute] = jsonFormat4(CertifiedAttribute.apply)
}
