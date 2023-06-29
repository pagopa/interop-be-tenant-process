package it.pagopa.interop.tenantprocess.server.impl

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.SecurityDirectives
import com.atlassian.oai.validator.report.ValidationReport
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import it.pagopa.interop.commons.cqrs.service.{MongoDbReadModelService, ReadModelService}
import it.pagopa.interop.commons.jwt.service.JWTReader
import it.pagopa.interop.commons.jwt.service.impl.{DefaultJWTReader, getClaimsVerifier}
import it.pagopa.interop.commons.jwt.{JWTConfiguration, KID, PublicKeysHolder, SerializedKey}
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.{Problem => CommonProblem}
import it.pagopa.interop.commons.utils.service.{OffsetDateTimeSupplier, UUIDSupplier}
import it.pagopa.interop.commons.utils.{AkkaUtils, OpenapiUtils}
import it.pagopa.interop.tenantprocess.api.impl.{
  HealthApiMarshallerImpl,
  HealthServiceApiImpl,
  TenantApiMarshallerImpl,
  TenantApiServiceImpl
}
import it.pagopa.interop.tenantprocess.api.{HealthApi, TenantApi}
import it.pagopa.interop.tenantprocess.common.system.ApplicationConfiguration
import it.pagopa.interop.tenantprocess.error.ResponseHandlers.serviceCode
import it.pagopa.interop.tenantprocess.service.impl._
import it.pagopa.interop.tenantprocess.service._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}

trait Dependencies {

  implicit val loggerTI: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog]("OAuth2JWTValidatorAsContexts")

  val uuidSupplier: UUIDSupplier               = UUIDSupplier
  val dateTimeSupplier: OffsetDateTimeSupplier = OffsetDateTimeSupplier

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

  implicit val readModelService: ReadModelService = new MongoDbReadModelService(
    ApplicationConfiguration.readModelConfig
  )

  val validationExceptionToRoute: ValidationReport => Route = report => {
    val error =
      CommonProblem(StatusCodes.BadRequest, OpenapiUtils.errorFromRequestValidationReport(report), serviceCode, None)
    complete(error.status, error)
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
      TenantApiServiceImpl(
        AttributeRegistryManagementServiceImpl,
        tenantManagement(blockingEc),
        agreementProcess(blockingEc),
        AgreementManagementServiceImpl,
        CatalogManagementServiceImpl,
        uuidSupplier,
        dateTimeSupplier
      ),
      TenantApiMarshallerImpl,
      jwtReader.OAuth2JWTValidatorAsContexts
    )

  private def tenantManagementInvoker(blockingEc: ExecutionContextExecutor)(implicit
    actorSystem: ActorSystem[_]
  ): TenantManagementInvoker =
    TenantManagementInvoker(blockingEc)(actorSystem.classicSystem)

  private final val tenantManagementApi: TenantManagementApi =
    TenantManagementApi(ApplicationConfiguration.tenantManagementURL)

  private final val tenantManagementAttributesApi: TenantManagementAttributesApi =
    TenantManagementAttributesApi(ApplicationConfiguration.tenantManagementURL)

  def tenantManagement(
    blockingEc: ExecutionContextExecutor
  )(implicit actorSystem: ActorSystem[_]): TenantManagementService =
    TenantManagementServiceImpl(
      tenantManagementInvoker(blockingEc),
      tenantManagementApi,
      tenantManagementAttributesApi
    )(blockingEc)

  private final val agreementProcessApi: AgreementProcessApi =
    AgreementProcessApi(ApplicationConfiguration.agreementProcessURL)

  private def agreementProcessInvoker(blockingEc: ExecutionContextExecutor)(implicit
    actorSystem: ActorSystem[_]
  ): AgreementProcessInvoker =
    AgreementProcessInvoker(blockingEc)(actorSystem.classicSystem)

  def agreementProcess(blockingEc: ExecutionContextExecutor)(implicit
    actorSystem: ActorSystem[_]
  ): AgreementProcessService =
    AgreementProcessServiceImpl(agreementProcessInvoker(blockingEc), agreementProcessApi, blockingEc)
}
