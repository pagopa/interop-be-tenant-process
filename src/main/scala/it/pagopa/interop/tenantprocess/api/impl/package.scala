package it.pagopa.interop.tenantprocess.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.interop.commons.utils.SprayCommonFormats._
import it.pagopa.interop.tenantprocess.model._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit def mailSeedFormat: RootJsonFormat[MailSeed] = jsonFormat3(MailSeed)
  implicit def mailFormat: RootJsonFormat[Mail]         = jsonFormat5(Mail)

  implicit def externalIdFormat: RootJsonFormat[ExternalId] = jsonFormat2(ExternalId)

  implicit def internalAttributeSeedFormat: RootJsonFormat[InternalAttributeSeed] = jsonFormat2(InternalAttributeSeed)
  implicit def internalTenantSeedFormat: RootJsonFormat[InternalTenantSeed]       = jsonFormat3(InternalTenantSeed)

  implicit def m2mAttributeSeedFormat: RootJsonFormat[M2MAttributeSeed]     = jsonFormat1(M2MAttributeSeed)
  implicit def m2mTenantSeedFormat: RootJsonFormat[M2MTenantSeed]           = jsonFormat3(M2MTenantSeed)
  implicit def selfcareTenantSeedFormat: RootJsonFormat[SelfcareTenantSeed] = jsonFormat6(SelfcareTenantSeed)
  implicit def certifiedTenantAttributeSeedFormat: RootJsonFormat[CertifiedTenantAttributeSeed] = jsonFormat1(
    CertifiedTenantAttributeSeed
  )

  implicit def certifierFormat: RootJsonFormat[Certifier]             = jsonFormat1(Certifier)
  implicit def tenantFeatureFormat: RootJsonFormat[TenantFeature]     = jsonFormat1(TenantFeature)
  implicit def tenantAttributeFormat: RootJsonFormat[TenantAttribute] = jsonFormat3(TenantAttribute)

  implicit def certifiedAttributeFormat: RootJsonFormat[CertifiedAttribute]           = jsonFormat4(CertifiedAttribute)
  implicit def certifiedAttributesFormat: RootJsonFormat[CertifiedAttributes]         = jsonFormat2(CertifiedAttributes)
  implicit def declaredTenantAttributeFormat: RootJsonFormat[DeclaredTenantAttribute] =
    jsonFormat3(DeclaredTenantAttribute)
  implicit def certifiedTenantAttributeFormat: RootJsonFormat[CertifiedTenantAttribute] =
    jsonFormat3(CertifiedTenantAttribute)
  implicit def verifiedTenantAttributeFormat: RootJsonFormat[VerifiedTenantAttribute]   =
    jsonFormat4(VerifiedTenantAttribute)

  implicit def declaredTenantAttributeSeedFormat: RootJsonFormat[DeclaredTenantAttributeSeed]             =
    jsonFormat1(DeclaredTenantAttributeSeed)
  implicit def verifiedTenantAttributeSeedFormat: RootJsonFormat[VerifiedTenantAttributeSeed]             =
    jsonFormat2(VerifiedTenantAttributeSeed)
  implicit def updateVerifiedTenantAttributeSeedFormat: RootJsonFormat[UpdateVerifiedTenantAttributeSeed] =
    jsonFormat1(UpdateVerifiedTenantAttributeSeed)

  implicit def tenantVerifierFormat: RootJsonFormat[TenantVerifier] = jsonFormat4(TenantVerifier)
  implicit def tenantRevokerFormat: RootJsonFormat[TenantRevoker]   = jsonFormat5(TenantRevoker)

  implicit def resourceIdFormat: RootJsonFormat[ResourceId]     = jsonFormat1(ResourceId)
  implicit def tenantFormat: RootJsonFormat[Tenant]             = jsonFormat12(Tenant)
  implicit def tenantsFormat: RootJsonFormat[Tenants]           = jsonFormat2(Tenants)
  implicit def problemErrorFormat: RootJsonFormat[ProblemError] = jsonFormat2(ProblemError)
  implicit def problemFormat: RootJsonFormat[Problem]           = jsonFormat6(Problem)

  final val entityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

}
