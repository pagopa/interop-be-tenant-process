package it.pagopa.interop.tenantprocess.api.adapters

import cats.implicits._
import it.pagopa.interop.tenantmanagement.client.model.{
  DeclaredTenantAttribute => DependencyDeclaredTenantAttribute,
  ExternalId => DependencyExternalId,
  TenantAttribute => DependencyTenantAttribute
}
import it.pagopa.interop.tenantprocess.model.{DeclaredTenantAttributeSeed, ExternalId}

import java.time.OffsetDateTime

object ApiAdapters {

  implicit class ExternalIdWrapper(private val id: ExternalId) extends AnyVal {
    def toDependency: DependencyExternalId = DependencyExternalId(origin = id.origin, value = id.value)
  }

  implicit class DeclaredTenantAttributeSeedWrapper(private val seed: DeclaredTenantAttributeSeed) extends AnyVal {
    def toDependency(now: OffsetDateTime): DependencyTenantAttribute = DependencyTenantAttribute(
      declared =
        DependencyDeclaredTenantAttribute(id = seed.id, assignmentTimestamp = now, revocationTimestamp = None).some,
      certified = None,
      verified = None
    )
  }

}
