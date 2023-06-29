package it.pagopa.interop.tenantprocess.service

import it.pagopa.interop.attributeregistrymanagement.model.persistence.attribute.PersistentAttribute
import it.pagopa.interop.commons.cqrs.service.ReadModelService

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}

trait AttributeRegistryManagementService {
  def getAttributeByExternalCode(origin: String, code: String)(implicit
    ec: ExecutionContext,
    readModel: ReadModelService
  ): Future[PersistentAttribute]
  def getAttributeById(
    attributeId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PersistentAttribute]
}
