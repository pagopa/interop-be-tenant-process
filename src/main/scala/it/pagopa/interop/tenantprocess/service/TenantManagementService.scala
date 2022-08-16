package it.pagopa.interop.tenantprocess.service

import it.pagopa.interop.tenantmanagement.client.model._

import java.util.UUID
import scala.concurrent.Future

trait TenantManagementService {
  def createTenant(seed: TenantSeed)(implicit contexts: Seq[(String, String)]): Future[Tenant]
  def updateTenant(tenantId: UUID, payload: TenantDelta)(implicit contexts: Seq[(String, String)]): Future[Tenant]

  def addTenantAttribute(tenantId: UUID, seed: TenantAttribute)(implicit
    contexts: Seq[(String, String)]
  ): Future[Tenant]

  def getTenant(tenantId: UUID)(implicit contexts: Seq[(String, String)]): Future[Tenant]
  def getTenantByExternalId(externalId: ExternalId)(implicit contexts: Seq[(String, String)]): Future[Tenant]
}
