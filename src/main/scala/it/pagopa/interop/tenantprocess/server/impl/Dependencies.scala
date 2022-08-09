package it.pagopa.interop.tenantprocess.server.impl

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.SecurityDirectives
import com.atlassian.oai.validator.report.ValidationReport
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import it.pagopa.interop.commons.jwt.service.JWTReader
import it.pagopa.interop.commons.jwt.service.impl.{DefaultJWTReader, getClaimsVerifier}
import it.pagopa.interop.commons.jwt.{JWTConfiguration, KID, PublicKeysHolder, SerializedKey}
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors
import it.pagopa.interop.commons.utils.{AkkaUtils, OpenapiUtils}
import it.pagopa.interop.tenantprocess.api.impl.{
  HealthApiMarshallerImpl,
  HealthServiceApiImpl,
  TenantApiMarshallerImpl,
  TenantApiServiceImpl,
  problemOf
}
import it.pagopa.interop.tenantprocess.api.{HealthApi, TenantApi}
import it.pagopa.interop.tenantprocess.common.system.ApplicationConfiguration
import it.pagopa.interop.tenantprocess.service._
import it.pagopa.interop.tenantprocess.service.impl._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

trait Dependencies {

  def jwtValidator(): Future[JWTReader] = JWTConfiguration.jwtReader
    .loadKeyset()
    .map(keyset =>
      new DefaultJWTReader with PublicKeysHolder {
        var publicKeyset: Map[KID, SerializedKey]                                        = keyset
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(audience = ApplicationConfiguration.jwtAudience)
      }
    )
    .toFuture

  val validationExceptionToRoute: ValidationReport => Route = report => {
    val error =
      problemOf(
        StatusCodes.BadRequest,
        GenericComponentErrors.ValidationRequestError(OpenapiUtils.errorFromRequestValidationReport(report))
      )
    complete(error.status, error)(HealthApiMarshallerImpl.toEntityMarshallerProblem)
  }

  val healthApi: HealthApi = new HealthApi(
    new HealthServiceApiImpl(),
    HealthApiMarshallerImpl,
    SecurityDirectives.authenticateOAuth2("SecurityRealm", AkkaUtils.PassThroughAuthenticator),
    loggingEnabled = false
  )

  def tenantApi(jwtReader: JWTReader, blockingEc: ExecutionContextExecutor)(implicit
    actorSystem: ActorSystem[_],
    ec: ExecutionContext
  ): TenantApi =
    new TenantApi(
      TenantApiServiceImpl(tenantManagement(blockingEc)),
      TenantApiMarshallerImpl,
      jwtReader.OAuth2JWTValidatorAsContexts
    )

  private def tenantManagementInvoker(blockingEc: ExecutionContextExecutor)(implicit
    actorSystem: ActorSystem[_]
  ): TenantManagementInvoker =
    TenantManagementInvoker(blockingEc)(actorSystem.classicSystem)

  private final val tenantManagementApi: TenantManagementApi =
    TenantManagementApi(ApplicationConfiguration.tenantManagementURL)

  def tenantManagement(
    blockingEc: ExecutionContextExecutor
  )(implicit actorSystem: ActorSystem[_], ec: ExecutionContext): TenantManagementService =
    TenantManagementServiceImpl(tenantManagementInvoker(blockingEc), tenantManagementApi)

}
