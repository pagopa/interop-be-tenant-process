package it.pagopa.interop.tenantprocess.api.adapters

import it.pagopa.interop.attributeregistrymanagement.client.model.Attribute
import it.pagopa.interop.tenantmanagement.client.model.{TenantAttribute, TenantAttributeKind}

import java.time.OffsetDateTime
import java.util.UUID

object AttributeRegistryManagementAdapters {

  implicit class AttributeWrapper(private val a: Attribute) extends AnyVal {
    def toCertifiedSeed(now: OffsetDateTime): TenantAttribute = TenantAttribute(
      id = UUID.fromString(a.id), // TODO This should be an UUID in attribute registry
      kind = TenantAttributeKind.CERTIFIED,
      assignmentTimestamp = now,
      revocationTimestamp = None,
      renewal = None,
      verifiedBy = None,
      revokedBy = None
    )
  }

}
