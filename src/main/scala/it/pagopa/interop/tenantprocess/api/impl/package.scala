package it.pagopa.interop.tenantprocess.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCode
import it.pagopa.interop.commons.jwt.service.JWTReader
import it.pagopa.interop.commons.utils.AkkaUtils.getFutureBearer
import it.pagopa.interop.commons.utils.SprayCommonFormats._
import it.pagopa.interop.commons.utils.TypeConversions.TryOps
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.tenantprocess.model._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  final val serviceErrorCodePrefix: String = "019"
  final val defaultProblemType: String     = "about:blank"

  implicit def externalIdFormat: RootJsonFormat[ExternalId] = jsonFormat2(ExternalId)

  implicit def internalAttributeSeedFormat: RootJsonFormat[InternalAttributeSeed] = jsonFormat2(InternalAttributeSeed)
  implicit def internalTenantSeedFormat: RootJsonFormat[InternalTenantSeed]       = jsonFormat2(InternalTenantSeed)

  implicit def m2mAttributeSeedFormat: RootJsonFormat[M2MAttributeSeed] = jsonFormat1(M2MAttributeSeed)
  implicit def m2mTenantSeedFormat: RootJsonFormat[M2MTenantSeed]       = jsonFormat2(M2MTenantSeed)

  implicit def selfcareTenantSeedFormat: RootJsonFormat[SelfcareTenantSeed] = jsonFormat2(SelfcareTenantSeed)

  implicit def tenantFormat: RootJsonFormat[Tenant]             = jsonFormat1(Tenant)
  implicit def problemErrorFormat: RootJsonFormat[ProblemError] = jsonFormat2(ProblemError)
  implicit def problemFormat: RootJsonFormat[Problem]           = jsonFormat5(Problem)

  def problemOf(httpError: StatusCode, error: ComponentError, defaultMessage: String = "Unknown error"): Problem =
    Problem(
      `type` = defaultProblemType,
      status = httpError.intValue,
      title = httpError.defaultMessage,
      errors = Seq(
        ProblemError(
          code = s"$serviceErrorCodePrefix-${error.code}",
          detail = Option(error.getMessage).getOrElse(defaultMessage)
        )
      )
    )

  def validateBearer(contexts: Seq[(String, String)], jwt: JWTReader)(implicit ec: ExecutionContext): Future[String] =
    for {
      bearer <- getFutureBearer(contexts)
      _      <- jwt.getClaims(bearer).toFuture
    } yield bearer
}
