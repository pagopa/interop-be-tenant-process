package it.pagopa.interop.tenantprocess.service

import it.pagopa.interop.attributeregistrymanagement.client.model.Attribute

import scala.concurrent.Future
import java.util.UUID

trait AttributeRegistryManagementService {

  def getAttributeByExternalCode(origin: String, code: String)(implicit
    contexts: Seq[(String, String)]
  ): Future[Attribute]

  def getAttributeById(id: UUID)(implicit contexts: Seq[(String, String)]): Future[Attribute]
}
