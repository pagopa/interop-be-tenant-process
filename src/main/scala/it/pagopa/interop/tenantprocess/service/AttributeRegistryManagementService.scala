package it.pagopa.interop.tenantprocess.service

import it.pagopa.interop.attributeregistrymanagement.client.model.Attribute

import scala.concurrent.Future

trait AttributeRegistryManagementService {

  def getAttributeByExternalCode(origin: String, code: String)(implicit
    contexts: Seq[(String, String)]
  ): Future[Attribute]
}
