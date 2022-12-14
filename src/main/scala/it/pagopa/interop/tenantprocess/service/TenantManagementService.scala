package it.pagopa.interop.tenantprocess.service

import it.pagopa.interop.tenantmanagement.client.model._

import java.util.UUID
import scala.concurrent.Future

trait TenantManagementService {
  def createTenant(seed: TenantSeed)(implicit contexts: Seq[(String, String)]): Future[Tenant]
  def updateTenant(tenantId: UUID, payload: TenantDelta)(implicit contexts: Seq[(String, String)]): Future[Tenant]

  def getTenantAttribute(tenantId: UUID, attributeId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[TenantAttribute]
  def addTenantAttribute(tenantId: UUID, seed: TenantAttribute)(implicit
    contexts: Seq[(String, String)]
  ): Future[Tenant]
  def updateTenantAttribute(tenantId: UUID, attributeId: UUID, attribute: TenantAttribute)(implicit
    contexts: Seq[(String, String)]
  ): Future[Tenant]

  def getTenant(tenantId: UUID)(implicit contexts: Seq[(String, String)]): Future[Tenant]
  def getTenantByExternalId(externalId: ExternalId)(implicit contexts: Seq[(String, String)]): Future[Tenant]
}
