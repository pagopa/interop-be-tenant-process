package it.pagopa.interop.tenantprocess.service

import it.pagopa.interop.tenantmanagement.client.model.{Tenant, TenantSeed}

import scala.concurrent.Future

trait TenantManagementService {
  def createTenant(seed: TenantSeed)(implicit contexts: Seq[(String, String)]): Future[Tenant]
}
