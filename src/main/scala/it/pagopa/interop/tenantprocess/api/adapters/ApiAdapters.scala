package it.pagopa.interop.tenantprocess.api.adapters

import cats.implicits._
import it.pagopa.interop.tenantmanagement.client.model.{
  DeclaredTenantAttribute => DependencyDeclaredTenantAttribute,
  ExternalId => DependencyExternalId,
  TenantAttribute => DependencyTenantAttribute,
  TenantVerifier => DependencyTenantVerifier,
  VerificationRenewal => DependencyVerificationRenewal,
  VerifiedTenantAttribute => DependencyVerifiedTenantAttribute,
  TenantFeature => DependencyTenantFeature,
  Certifier => DependencyCertifier,
  MailKind => DependencyMailKind,
  MailSeed => DependencyMailSeed,
  TenantDelta => DependencyTenantDelta
}
import it.pagopa.interop.tenantprocess.model.VerificationRenewal._
import it.pagopa.interop.tenantprocess.model.{
  DeclaredTenantAttributeSeed,
  ExternalId,
  VerificationRenewal,
  TenantFeature,
  Certifier,
  VerifiedTenantAttributeSeed,
  TenantDelta,
  Mail,
  MailKind
}

import java.time.OffsetDateTime
import java.util.UUID
import it.pagopa.interop.tenantmanagement.client.model.MailKind.CONTACT_EMAIL
import it.pagopa.interop.tenantprocess.model.MailSeed

object ApiAdapters {

  implicit class MailWrapper(private val m: Mail) extends AnyVal {
    def fromAPI: DependencyMailSeed =
      DependencyMailSeed(kind = m.kind.fromAPI, address = m.address, description = m.description)
  }

  implicit class MailKindWrapper(private val mk: MailKind) extends AnyVal {
    def fromAPI: DependencyMailKind = mk match {
      case MailKind.CONTACT_EMAIL => CONTACT_EMAIL
    }
  }

  implicit class TenantFeatureWrapper(private val tf: TenantFeature) extends AnyVal {
    def fromAPI: DependencyTenantFeature = DependencyTenantFeature(tf.certifier.map(_.fromAPI))
  }

  implicit class CertifierWrapper(private val c: Certifier) extends AnyVal {
    def fromAPI: DependencyCertifier = DependencyCertifier(c.certifierId)
  }

  implicit class TenantDeltaWrapper(private val td: TenantDelta) extends AnyVal {
    def fromAPI(selfcareId: Option[String], features: Seq[DependencyTenantFeature]): DependencyTenantDelta =
      DependencyTenantDelta(selfcareId, features, mails = td.mails.map(_.toDependency))
  }

  implicit class MailSeedWrapper(private val ms: MailSeed) extends AnyVal {
    def toDependency: DependencyMailSeed =
      DependencyMailSeed(kind = ms.kind.fromAPI, address = ms.address, description = ms.description)
  }

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
