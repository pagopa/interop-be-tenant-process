package it.pagopa.interop.tenantprocess.api.adapters

import cats.implicits._
import it.pagopa.interop.attributeregistrymanagement.client.model.Attribute
import it.pagopa.interop.tenantmanagement.client.model.{TenantAttribute, CertifiedTenantAttribute}

import java.time.OffsetDateTime
import java.util.UUID

object AttributeRegistryManagementAdapters {

  implicit class AttributeWrapper(private val a: Attribute) extends AnyVal {
    def toCertifiedSeed(now: OffsetDateTime): TenantAttribute = TenantAttribute(certified =
      CertifiedTenantAttribute(
        id = UUID.fromString(a.id), // TODO This should be an UUID in attribute registry
        assignmentTimestamp = now,
        revocationTimestamp = None
      ).some
    )
  }

}
