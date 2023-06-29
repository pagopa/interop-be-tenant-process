package it.pagopa.interop.tenantprocess.service.impl

import it.pagopa.interop.tenantprocess.service.AttributeRegistryManagementService
import it.pagopa.interop.attributeregistrymanagement.model.persistence.attribute.PersistentAttribute
import it.pagopa.interop.tenantprocess.common.readmodel.ReadModelAttributeRegistryQueries
import it.pagopa.interop.tenantprocess.error.TenantProcessErrors.{
  RegistryAttributeNotFound,
  RegistryAttributeIdNotFound
}
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.cqrs.service.ReadModelService

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}

final object AttributeRegistryManagementServiceImpl extends AttributeRegistryManagementService {

  override def getAttributeByExternalCode(origin: String, code: String)(implicit
    ec: ExecutionContext,
    readModel: ReadModelService
  ): Future[PersistentAttribute] =
    ReadModelAttributeRegistryQueries
      .getAttributeByExternalCode(origin, code)
      .flatMap(_.toFuture(RegistryAttributeNotFound(origin, code)))

  override def getAttributeById(
    attributeId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PersistentAttribute] =
    ReadModelAttributeRegistryQueries
      .getAttributeById(attributeId)
      .flatMap(_.toFuture(RegistryAttributeIdNotFound(attributeId)))
}
