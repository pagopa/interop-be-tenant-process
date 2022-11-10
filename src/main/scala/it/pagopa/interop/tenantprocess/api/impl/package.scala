package it.pagopa.interop.tenantprocess.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCode
import it.pagopa.interop.commons.utils.SprayCommonFormats._
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.tenantprocess.model._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit def mailSeedFormat: RootJsonFormat[MailSeed]       = jsonFormat3(MailSeed)
  implicit def mailFormat: RootJsonFormat[Mail]               = jsonFormat4(Mail)
  implicit def tenantDeltaFormat: RootJsonFormat[TenantDelta] = jsonFormat3(TenantDelta)

  implicit def externalIdFormat: RootJsonFormat[ExternalId] = jsonFormat2(ExternalId)

  implicit def internalAttributeSeedFormat: RootJsonFormat[InternalAttributeSeed] = jsonFormat2(InternalAttributeSeed)
  implicit def internalTenantSeedFormat: RootJsonFormat[InternalTenantSeed]       = jsonFormat2(InternalTenantSeed)

  implicit def m2mAttributeSeedFormat: RootJsonFormat[M2MAttributeSeed] = jsonFormat1(M2MAttributeSeed)
  implicit def m2mTenantSeedFormat: RootJsonFormat[M2MTenantSeed]       = jsonFormat2(M2MTenantSeed)

  implicit def selfcareTenantSeedFormat: RootJsonFormat[SelfcareTenantSeed] = jsonFormat2(SelfcareTenantSeed)

  implicit def certifierFormat: RootJsonFormat[Certifier]             = jsonFormat1(Certifier)
  implicit def tenantFeatureFormat: RootJsonFormat[TenantFeature]     = jsonFormat1(TenantFeature)
  implicit def tenantAttributeFormat: RootJsonFormat[TenantAttribute] = jsonFormat3(TenantAttribute)

  implicit def declaredTenantAttributeFormat: RootJsonFormat[DeclaredTenantAttribute]   =
    jsonFormat3(DeclaredTenantAttribute)
  implicit def certifiedTenantAttributeFormat: RootJsonFormat[CertifiedTenantAttribute] =
    jsonFormat3(CertifiedTenantAttribute)
  implicit def verifiedTenantAttributeFormat: RootJsonFormat[VerifiedTenantAttribute]   =
    jsonFormat4(VerifiedTenantAttribute)

  implicit def declaredTenantAttributeSeedFormat: RootJsonFormat[DeclaredTenantAttributeSeed] =
    jsonFormat1(DeclaredTenantAttributeSeed)
  implicit def verifiedTenantAttributeSeedFormat: RootJsonFormat[VerifiedTenantAttributeSeed] =
    jsonFormat3(VerifiedTenantAttributeSeed)

  implicit def verificationRenewalFormat: RootJsonFormat[VerificationRenewal] =
    VerificationRenewal.VerificationRenewalFormat

  implicit def tenantVerifierFormat: RootJsonFormat[TenantVerifier] = jsonFormat5(TenantVerifier)
  implicit def tenantRevokerFormat: RootJsonFormat[TenantRevoker]   = jsonFormat6(TenantRevoker)

  implicit def tenantFormat: RootJsonFormat[Tenant]             = jsonFormat8(Tenant)
  implicit def problemErrorFormat: RootJsonFormat[ProblemError] = jsonFormat2(ProblemError)
  implicit def problemFormat: RootJsonFormat[Problem]           = jsonFormat5(Problem)

  final val entityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  final val serviceErrorCodePrefix: String = "019"
  final val defaultProblemType: String     = "about:blank"
  final val defaultErrorMessage: String    = "Unknown error"

  def problemOf(httpError: StatusCode, error: ComponentError): Problem =
    Problem(
      `type` = defaultProblemType,
      status = httpError.intValue,
      title = httpError.defaultMessage,
      errors = Seq(
        ProblemError(
          code = s"$serviceErrorCodePrefix-${error.code}",
          detail = Option(error.getMessage).getOrElse(defaultErrorMessage)
        )
      )
    )

  def problemOf(httpError: StatusCode, errors: List[ComponentError]): Problem =
    Problem(
      `type` = defaultProblemType,
      status = httpError.intValue,
      title = httpError.defaultMessage,
      errors = errors.map(error =>
        ProblemError(
          code = s"$serviceErrorCodePrefix-${error.code}",
          detail = Option(error.getMessage).getOrElse(defaultErrorMessage)
        )
      )
    )

}
