package it.pagopa.interop.tenantprocess.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.interop.tenantprocess.api.TenantApiMarshaller
import it.pagopa.interop.tenantprocess.model._
import spray.json._

object TenantApiMarshallerImpl extends TenantApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  override implicit def fromEntityUnmarshallerTenantDelta: FromEntityUnmarshaller[TenantDelta] =
    sprayJsonUnmarshaller[TenantDelta]

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = entityMarshallerProblem

  override implicit def toEntityMarshallerTenant: ToEntityMarshaller[Tenant] = sprayJsonMarshaller[Tenant]

  implicit def fromEntityUnmarshallerTenant: FromEntityUnmarshaller[Tenant] = sprayJsonUnmarshaller[Tenant]

  override implicit def fromEntityUnmarshallerInternalTenantSeed: FromEntityUnmarshaller[InternalTenantSeed] =
    sprayJsonUnmarshaller[InternalTenantSeed]

  override implicit def fromEntityUnmarshallerSelfcareTenantSeed: FromEntityUnmarshaller[SelfcareTenantSeed] =
    sprayJsonUnmarshaller[SelfcareTenantSeed]

  override implicit def fromEntityUnmarshallerM2MTenantSeed: FromEntityUnmarshaller[M2MTenantSeed] =
    sprayJsonUnmarshaller[M2MTenantSeed]

  override implicit def fromEntityUnmarshallerDeclaredTenantAttributeSeed
    : FromEntityUnmarshaller[DeclaredTenantAttributeSeed] = sprayJsonUnmarshaller[DeclaredTenantAttributeSeed]

  override implicit def fromEntityUnmarshallerVerifiedTenantAttributeSeed
    : FromEntityUnmarshaller[VerifiedTenantAttributeSeed] = sprayJsonUnmarshaller[VerifiedTenantAttributeSeed]

  override implicit def toEntityMarshallerTenants: ToEntityMarshaller[Tenants] = sprayJsonMarshaller[Tenants]

  override implicit def fromEntityUnmarshallerUpdateVerifiedTenantAttributeSeed
    : FromEntityUnmarshaller[UpdateVerifiedTenantAttributeSeed] =
    sprayJsonUnmarshaller[UpdateVerifiedTenantAttributeSeed]

}
