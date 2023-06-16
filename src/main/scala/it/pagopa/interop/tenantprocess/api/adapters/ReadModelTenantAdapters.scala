package it.pagopa.interop.tenantprocess.api.adapters

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import cats.syntax.all._
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantFeature.PersistentCertifier
import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.tenantprocess.model._
import spray.json._

object ReadModelTenantAdapters extends SprayJsonSupport with DefaultJsonProtocol {

  implicit class PersistentTenantWrapper(private val t: PersistentTenant) extends AnyVal {
    def toApi: Tenant = Tenant(
      id = t.id,
      name = t.name,
      selfcareId = t.selfcareId,
      externalId = t.externalId.toApi,
      features = t.features.map(_.toApi),
      attributes = t.attributes.map(_.toApi),
      createdAt = t.createdAt,
      updatedAt = t.updatedAt,
      mails = t.mails.map(_.toApi)
    )
  }

  implicit class PersistentMailWrapper(private val m: PersistentTenantMail) extends AnyVal {
    def toApi: Mail = Mail(kind = m.kind.toApi, address = m.address, createdAt = m.createdAt)
  }

  implicit class PersistentMailKindWrapper(private val k: PersistentTenantMailKind) extends AnyVal {
    def toApi: MailKind = k match {
      case PersistentTenantMailKind.ContactEmail => MailKind.CONTACT_EMAIL
    }
  }

  implicit class PersistentExternalIdWrapper(private val t: PersistentExternalId) extends AnyVal {
    def toApi: ExternalId = ExternalId(origin = t.origin, value = t.value)
  }

  implicit class PersistentCertifierWrapper(private val t: PersistentCertifier) extends AnyVal {
    def toApi: Certifier = Certifier(certifierId = t.certifierId)
  }

  implicit class PersistentTenantFeatureWrapper(private val t: PersistentTenantFeature) extends AnyVal {
    def toApi: TenantFeature = t match {
      case PersistentCertifier(certifierId) => TenantFeature(certifier = Certifier(certifierId).some)
    }
  }

  implicit class PersistentTenantAttributeWrapper(private val pa: PersistentTenantAttribute) extends AnyVal {
    def toApi: TenantAttribute = pa match {
      case a: PersistentCertifiedAttribute => TenantAttribute(certified = a.toApi.some)
      case a: PersistentDeclaredAttribute  => TenantAttribute(declared = a.toApi.some)
      case a: PersistentVerifiedAttribute  => TenantAttribute(verified = a.toApi.some)
    }
  }

  implicit class PersistentDeclaredTenantAttributeWrapper(private val t: PersistentDeclaredAttribute) extends AnyVal {
    def toApi: DeclaredTenantAttribute = DeclaredTenantAttribute(
      id = t.id,
      assignmentTimestamp = t.assignmentTimestamp,
      revocationTimestamp = t.revocationTimestamp
    )
  }

  implicit class PersistentCertifiedTenantAttributeWrapper(private val t: PersistentCertifiedAttribute) extends AnyVal {
    def toApi: CertifiedTenantAttribute = CertifiedTenantAttribute(
      id = t.id,
      assignmentTimestamp = t.assignmentTimestamp,
      revocationTimestamp = t.revocationTimestamp
    )
  }

  implicit class PersistentVerifiedTenantAttributeWrapper(private val t: PersistentVerifiedAttribute) extends AnyVal {
    def toApi: VerifiedTenantAttribute = VerifiedTenantAttribute(
      id = t.id,
      assignmentTimestamp = t.assignmentTimestamp,
      verifiedBy = t.verifiedBy.map(_.toApi),
      revokedBy = t.revokedBy.map(_.toApi)
    )
  }

  implicit class PersistentTenantVerifierWrapper(private val t: PersistentTenantVerifier) extends AnyVal {
    def toApi: TenantVerifier = TenantVerifier(
      id = t.id,
      verificationDate = t.verificationDate,
      expirationDate = t.expirationDate,
      extensionDate = t.extensionDate
    )
  }

  implicit class PersistentTenantRevokerWrapper(private val t: PersistentTenantRevoker) extends AnyVal {
    def toApi: TenantRevoker = TenantRevoker(
      id = t.id,
      verificationDate = t.verificationDate,
      expirationDate = t.expirationDate,
      extensionDate = t.extensionDate,
      revocationDate = t.revocationDate
    )
  }

}
