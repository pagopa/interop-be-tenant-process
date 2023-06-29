package it.pagopa.interop.tenantprocess.service

import it.pagopa.interop.tenantmanagement.client.model._
import it.pagopa.interop.tenantmanagement.model.tenant.{
  PersistentTenant,
  PersistentTenantAttribute,
  PersistentExternalId
}
import it.pagopa.interop.commons.cqrs.service.ReadModelService

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}

trait TenantManagementService {
  def createTenant(seed: TenantSeed)(implicit contexts: Seq[(String, String)]): Future[Tenant]
  def updateTenant(tenantId: UUID, payload: TenantDelta)(implicit contexts: Seq[(String, String)]): Future[Tenant]

  def getTenantAttribute(tenantId: UUID, attributeId: UUID)(implicit
    ec: ExecutionContext,
    readModel: ReadModelService
  ): Future[PersistentTenantAttribute]
  def addTenantAttribute(tenantId: UUID, seed: TenantAttribute)(implicit
    contexts: Seq[(String, String)]
  ): Future[Tenant]
  def updateTenantAttribute(tenantId: UUID, attributeId: UUID, attribute: TenantAttribute)(implicit
    contexts: Seq[(String, String)]
  ): Future[Tenant]

  def getTenantById(
    tenantId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PersistentTenant]
  def getTenantByExternalId(
    externalId: PersistentExternalId
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PersistentTenant]
}
