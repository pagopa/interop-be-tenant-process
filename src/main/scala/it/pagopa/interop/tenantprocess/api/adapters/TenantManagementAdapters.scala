package it.pagopa.interop.tenantprocess.api.adapters

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.interop.tenantmanagement.client.model.{
  Problem => DependencyProblem,
  ProblemError => DependencyProblemError,
  Tenant => DependencyTenant,
  ExternalId => DependencyExternalId,
  TenantFeature => DependencyTenantFeature,
  Certifier => DependencyCertifier,
  TenantAttribute => DependencyTenantAttribute,
  DeclaredTenantAttribute => DependencyDeclaredTenantAttribute,
  CertifiedTenantAttribute => DependencyCertifiedTenantAttribute,
  VerifiedTenantAttribute => DependencyVerifiedTenantAttribute,
  VerificationRenewal => DependencyVerificationRenewal,
  TenantVerifier => DependencyTenantVerifier,
  TenantRevoker => DependencyTenantRevoker
}
import it.pagopa.interop.tenantprocess.model._
import spray.json._

import scala.util.Try
import it.pagopa.interop.tenantmanagement.client.model.VerificationRenewal.AUTOMATIC_RENEWAL
import it.pagopa.interop.tenantmanagement.client.model.VerificationRenewal.REVOKE_ON_EXPIRATION

object TenantManagementAdapters extends SprayJsonSupport with DefaultJsonProtocol {
  implicit def problemErrorFormat: RootJsonFormat[DependencyProblemError] = jsonFormat2(DependencyProblemError)
  implicit def problemFormat: RootJsonFormat[DependencyProblem]           = jsonFormat5(DependencyProblem)

  implicit class DependencyTenantWrapper(private val t: DependencyTenant) extends AnyVal {
    def toApi: Tenant = Tenant(
      id = t.id,
      selfcareId = t.selfcareId,
      externalId = t.externalId.toApi,
      features = t.features.map(_.toApi),
      attributes = t.attributes.map(_.toApi),
      createdAt = t.createdAt,
      updatedAt = t.updatedAt
    )
  }

  implicit class DependencyExternalIdWrapper(private val t: DependencyExternalId) extends AnyVal {
    def toApi: ExternalId = ExternalId(origin = t.origin, value = t.value)
  }

  implicit class DependencyCertifierWrapper(private val t: DependencyCertifier) extends AnyVal {
    def toApi: Certifier = Certifier(certifierId = t.certifierId)
  }

  implicit class DependencyTenantFeatureWrapper(private val t: DependencyTenantFeature) extends AnyVal {
    def toApi: TenantFeature = TenantFeature(certifier = t.certifier.map(_.toApi))
  }

  implicit class DependencyTenantAttributeWrapper(private val t: DependencyTenantAttribute) extends AnyVal {
    def toApi: TenantAttribute = TenantAttribute(
      declared = t.declared.map(_.toApi),
      certified = t.certified.map(_.toApi),
      verified = t.verified.map(_.toApi)
    )
  }

  implicit class DependencyDeclaredTenantAttributeWrapper(private val t: DependencyDeclaredTenantAttribute)
      extends AnyVal {
    def toApi: DeclaredTenantAttribute = DeclaredTenantAttribute(
      id = t.id,
      assignmentTimestamp = t.assignmentTimestamp,
      revocationTimestamp = t.revocationTimestamp
    )
  }

  implicit class DependencyCertifiedTenantAttributeWrapper(private val t: DependencyCertifiedTenantAttribute)
      extends AnyVal {
    def toApi: CertifiedTenantAttribute = CertifiedTenantAttribute(
      id = t.id,
      assignmentTimestamp = t.assignmentTimestamp,
      revocationTimestamp = t.revocationTimestamp
    )
  }

  implicit class DependencyVerifiedTenantAttributeWrapper(private val t: DependencyVerifiedTenantAttribute)
      extends AnyVal {
    def toApi: VerifiedTenantAttribute = VerifiedTenantAttribute(
      id = t.id,
      assignmentTimestamp = t.assignmentTimestamp,
      renewal = t.renewal.toApi,
      verifiedBy = t.verifiedBy.map(_.toApi),
      revokedBy = t.revokedBy.map(_.toApi)
    )
  }

  implicit class DependencyVerificationRenewalWrapper(private val t: DependencyVerificationRenewal) extends AnyVal {
    def toApi: VerificationRenewal = t match {
      case AUTOMATIC_RENEWAL    => VerificationRenewal.AUTOMATIC_RENEWAL
      case REVOKE_ON_EXPIRATION => VerificationRenewal.REVOKE_ON_EXPIRATION
    }
  }

  implicit class DependencyTenantVerifierWrapper(private val t: DependencyTenantVerifier) extends AnyVal {
    def toApi: TenantVerifier = TenantVerifier(
      id = t.id,
      verificationDate = t.verificationDate,
      expirationDate = t.expirationDate,
      extentionDate = t.extentionDate
    )
  }

  implicit class DependencyTenantRevokerWrapper(private val t: DependencyTenantRevoker) extends AnyVal {
    def toApi: TenantRevoker = TenantRevoker(
      id = t.id,
      verificationDate = t.verificationDate,
      expirationDate = t.expirationDate,
      extentionDate = t.extentionDate,
      revocationDate = t.revocationDate
    )
  }

  implicit class ProblemObjectWrapper(private val t: DependencyProblem.type) extends AnyVal {
    def fromString(body: String): Try[Problem] =
      Try(body.parseJson.convertTo[DependencyProblem]).map(problem =>
        Problem(
          `type` = problem.`type`,
          status = problem.status,
          title = problem.title,
          detail = problem.detail,
          errors = problem.errors.map(_.toApi)
        )
      )
  }
  implicit class ProblemWrapper(private val t: DependencyProblem)            extends AnyVal {
    def toApi: Problem =
      Problem(`type` = t.`type`, status = t.status, title = t.title, detail = t.detail, errors = t.errors.map(_.toApi))
  }

  implicit class ProblemErrorWrapper(private val t: DependencyProblemError) extends AnyVal {
    def toApi: ProblemError = ProblemError(code = t.code, detail = t.detail)
  }

}
