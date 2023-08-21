package it.pagopa.interop.tenantprocess.api.adapters

import cats.implicits._
import it.pagopa.interop.attributeregistrymanagement.model.persistence.attribute.PersistentAttribute
import it.pagopa.interop.tenantmanagement.client.model.{CertifiedTenantAttribute, TenantAttribute}

import java.time.OffsetDateTime

object AttributeRegistryManagementAdapters {

  implicit class AttributeWrapper(private val a: PersistentAttribute) extends AnyVal {
    def toCertifiedSeed(now: OffsetDateTime): TenantAttribute = TenantAttribute(certified =
      CertifiedTenantAttribute(id = a.id, assignmentTimestamp = now, revocationTimestamp = None).some
    )
  }
}
