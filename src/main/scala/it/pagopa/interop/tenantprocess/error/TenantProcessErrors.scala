package it.pagopa.interop.tenantprocess.error

import it.pagopa.interop.commons.utils.errors.ComponentError

import java.util.UUID
import java.time.OffsetDateTime

object TenantProcessErrors {
  final case class TenantIsNotACertifier(tenantId: UUID)
      extends ComponentError("0001", s"Organization ${tenantId.toString} not allowed to assign attributes")

  final case class SelfcareIdConflict(tenantId: UUID, existingSelfcareId: String, newSelfcareId: String)
      extends ComponentError(
        "0002",
        s"Conflict on Tenant SelfCare ID update for tenant $tenantId: old value $existingSelfcareId - new value $newSelfcareId"
      )

  final case class DeclaredAttributeNotFound(tenantId: UUID, attributeId: String)
      extends ComponentError("0003", s"Attribute $attributeId not found for Tenant $tenantId")

  object VerifiedAttributeSelfVerification
      extends ComponentError("0004", s"Organizations are not allowed to verify own attributes")

  final case class AttributeVerificationNotAllowed(consumerId: UUID, attributeId: UUID)
      extends ComponentError(
        "0005",
        s"Organization is not allowed to verify attribute $attributeId for tenant $consumerId"
      )

  final case class AttributeAlreadyRevoked(targetTenantId: UUID, requesterTenantId: UUID, attributeId: UUID)
      extends ComponentError(
        "0006",
        s"Attribute $attributeId has been already revoked for $targetTenantId by $requesterTenantId"
      )

  object VerifiedAttributeSelfRevocation
      extends ComponentError("0007", s"Organizations are not allowed to revoke own attributes")

  final case class AttributeRevocationNotAllowed(consumerId: UUID, attributeId: UUID)
      extends ComponentError(
        "0008",
        s"Organization is not allowed to revoke attribute $attributeId for tenant $consumerId"
      )

  final case class RegistryAttributeNotFound(origin: String, value: String)
      extends ComponentError("0009", s"Attribute $origin/$value not found in registry")

  final case class TenantNotFound(origin: String, value: String)
      extends ComponentError("0010", s"Tenant $origin/$value not found")

  final case class TenantByIdNotFound(tenantId: UUID) extends ComponentError("0011", s"Tenant $tenantId not found")

  final case class CertifiedAttributeNotFoundInTenant(tenantId: UUID, attributeOrigin: String, attributeCode: String)
      extends ComponentError(
        "0012",
        s"Certified Attribute ($attributeOrigin, $attributeCode) not found in tenant $tenantId"
      )

  final case class DeclaredAttributeNotFoundInTenant(tenantId: UUID, attributeId: UUID)
      extends ComponentError("0013", s"Declared Attribute $attributeId not found in tenant $tenantId")

  final case class VerifiedAttributeNotFoundInTenant(tenantId: UUID, attributeId: UUID)
      extends ComponentError("0014", s"Verified Attribute $attributeId not found in tenant $tenantId")

  final case class TenantAttributeNotFound(tenantId: UUID, attributeId: UUID)
      extends ComponentError("0015", s"Attribute $attributeId not found for Tenant $tenantId")

  final case class ExpirationDateCannotBeInThePast(date: OffsetDateTime)
      extends ComponentError("0016", s"Expiration date $date cannot be in the past")

  final case class OrganizationNotFoundInVerifiers(requesterId: UUID, tenantId: UUID, attributeId: UUID)
      extends ComponentError(
        "0017",
        s"Organization $requesterId not found in verifier for Tenant $tenantId and attribute $attributeId"
      )

  final case class ExpirationDateNotFoundInVerifier(tenantId: UUID, attributeId: UUID, verifierId: UUID)
      extends ComponentError(
        "0018",
        s"ExpirationDate not found in verifier $verifierId for Tenant $tenantId and attribute $attributeId"
      )

  final case class RegistryAttributeIdNotFound(id: UUID)
      extends ComponentError("0019", s"Attribute ${id.toString} not found in registry")

  final case class EServiceNotFound(id: UUID)
      extends ComponentError("0020", s"EService ${id.toString} not found in the catalog")

  final case class SelcareIdNotFound(selfcareId: UUID)
      extends ComponentError("0021", s"Tenant with selfcare id ${selfcareId.toString} not found in the catalog")

  final case class CertifiedAttributeAlreadyInTenant(tenantId: UUID, attributeOrigin: String, attributeCode: String)
      extends ComponentError(
        "0022",
        s"Certified Attribute ($attributeOrigin, $attributeCode) already in tenant $tenantId"
      )
}
