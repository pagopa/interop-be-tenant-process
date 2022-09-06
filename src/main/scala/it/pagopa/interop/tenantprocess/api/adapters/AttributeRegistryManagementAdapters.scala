package it.pagopa.interop.tenantprocess.api.adapters

import cats.implicits._
import it.pagopa.interop.attributeregistrymanagement.client.model.Attribute
import it.pagopa.interop.tenantmanagement.client.model.{CertifiedTenantAttribute, TenantAttribute}

import java.time.OffsetDateTime

object AttributeRegistryManagementAdapters {

  implicit class AttributeWrapper(private val a: Attribute) extends AnyVal {
    def toCertifiedSeed(now: OffsetDateTime): TenantAttribute = TenantAttribute(certified =
      CertifiedTenantAttribute(id = a.id, assignmentTimestamp = now, revocationTimestamp = None).some
    )
  }

}
