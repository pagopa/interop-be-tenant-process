package it.pagopa.interop.tenantprocess.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.interop.tenantprocess.api.TenantApiMarshaller
import it.pagopa.interop.tenantprocess.model._
import spray.json._

object TenantApiMarshallerImpl extends TenantApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {
  override implicit def fromEntityUnmarshallerTenantSeed: FromEntityUnmarshaller[TenantSeed] =
    sprayJsonUnmarshaller[TenantSeed]

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  override implicit def toEntityMarshallerTenant: ToEntityMarshaller[Tenant] = sprayJsonMarshaller[Tenant]
}
