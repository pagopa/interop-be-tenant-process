package it.pagopa.interop.tenantprocess.service

import it.pagopa.interop.tenantmanagement.client.model.{ExternalId, Tenant, TenantSeed}

import java.util.UUID
import scala.concurrent.Future

// TODO Mock, remove once ready
case class TenantAttributeSeed(id: String)
case class TenantUpdatePayload(selfcareId: String)

trait TenantManagementService {
  def createTenant(seed: TenantSeed)(implicit contexts: Seq[(String, String)]): Future[Tenant]
  def updateTenant(tenantId: UUID, payload: TenantUpdatePayload)(implicit
    contexts: Seq[(String, String)]
  ): Future[Tenant]

  def addTenantAttribute(tenantId: UUID, seed: TenantAttributeSeed)(implicit
    contexts: Seq[(String, String)]
  ): Future[Tenant]

  def getTenant(tenantId: UUID)(implicit contexts: Seq[(String, String)]): Future[Tenant]
  def getTenantByExternalId(externalId: ExternalId)(implicit contexts: Seq[(String, String)]): Future[Tenant]
}
