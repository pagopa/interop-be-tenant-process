package it.pagopa.interop.tenantprocess.error

import it.pagopa.interop.commons.utils.errors.ComponentError

import java.util.UUID

object TenantProcessErrors {
  final case class TenantIsNotACertifier(tenantId: UUID)
      extends ComponentError("0001", s"Organization ${tenantId.toString()} not allowed to assign attributes")

  final case class SelfcareIdConflict(tenantId: UUID, existingSelfcareId: String, newSelfcareId: String)
      extends ComponentError(
        "0002",
        s"Conflict on Tenant SelfCare ID update for tenant $tenantId: old value $existingSelfcareId - new value $newSelfcareId"
      )

  final case class CertifiedAttributeNotFound(origin: String, attributeCode: String)
      extends ComponentError("0003", s"Attribute ($origin, $attributeCode) not found")

  final case class DeclaredAttributeNotFound(tenantId: UUID, attributeId: String)
      extends ComponentError("0004", s"Attribute $attributeId not found for Tenant $tenantId")

}
