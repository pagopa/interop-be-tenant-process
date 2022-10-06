package it.pagopa.interop.tenantprocess.api.adapters

import cats.implicits._
import it.pagopa.interop.tenantmanagement.client.model.{
  DeclaredTenantAttribute => DependencyDeclaredTenantAttribute,
  ExternalId => DependencyExternalId,
  TenantAttribute => DependencyTenantAttribute,
  TenantVerifier => DependencyTenantVerifier,
  VerificationRenewal => DependencyVerificationRenewal,
  VerifiedTenantAttribute => DependencyVerifiedTenantAttribute
}
import it.pagopa.interop.tenantprocess.model.VerificationRenewal._
import it.pagopa.interop.tenantprocess.model.{
  DeclaredTenantAttributeSeed,
  ExternalId,
  VerificationRenewal,
  VerifiedTenantAttributeSeed
}

import java.time.OffsetDateTime
import java.util.UUID

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

  implicit class VerifiedTenantAttributeSeedWrapper(private val seed: VerifiedTenantAttributeSeed) extends AnyVal {
    def toCreateDependency(now: OffsetDateTime, requesterId: UUID): DependencyTenantAttribute =
      DependencyTenantAttribute(
        declared = None,
        certified = None,
        verified = DependencyVerifiedTenantAttribute(
          id = seed.id,
          assignmentTimestamp = now,
          verifiedBy = Seq(
            DependencyTenantVerifier(
              id = requesterId,
              verificationDate = now,
              renewal = seed.renewal.toDependency,
              expirationDate = seed.expirationDate,
              extensionDate = None
            )
          ),
          revokedBy = Nil
        ).some
      )

    def toUpdateDependency(
      now: OffsetDateTime,
      requesterId: UUID,
      attribute: DependencyVerifiedTenantAttribute
    ): DependencyTenantAttribute = DependencyTenantAttribute(
      declared = None,
      certified = None,
      verified = DependencyVerifiedTenantAttribute(
        id = seed.id,
        assignmentTimestamp = attribute.assignmentTimestamp,
        verifiedBy = attribute.verifiedBy :+
          DependencyTenantVerifier(
            id = requesterId,
            verificationDate = now,
            renewal = seed.renewal.toDependency,
            expirationDate = seed.expirationDate,
            extensionDate = None
          ),
        revokedBy = attribute.revokedBy
      ).some
    )
  }

  implicit class VerificationRenewalWrapper(private val t: VerificationRenewal) extends AnyVal {
    def toDependency: DependencyVerificationRenewal = t match {
      case AUTOMATIC_RENEWAL    => DependencyVerificationRenewal.AUTOMATIC_RENEWAL
      case REVOKE_ON_EXPIRATION => DependencyVerificationRenewal.REVOKE_ON_EXPIRATION
    }
  }

}
