package it.pagopa.interop.tenantprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.{Route, StandardRoute}
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.jwt._
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils.getUidFuture
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.{GenericError, OperationForbidden}
import it.pagopa.interop.tenantmanagement.client.invoker.{ApiError => TenantApiError}
import it.pagopa.interop.tenantmanagement.client.model.{Problem => TenantProblem}
import it.pagopa.interop.tenantprocess.api.TenantApiService
import it.pagopa.interop.tenantprocess.api.adapters.ApiAdapters._
import it.pagopa.interop.tenantprocess.api.adapters.TenantManagementAdapters._
import it.pagopa.interop.tenantprocess.model._
import it.pagopa.interop.tenantprocess.service._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

final case class TenantApiServiceImpl(tenantManagementService: TenantManagementService)(implicit ec: ExecutionContext)
    extends TenantApiService {

  private val logger = Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  private[this] def authorize(roles: String*)(
    route: => Route
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    authorizeInterop(hasPermissions(roles: _*), problemOf(StatusCodes.Forbidden, OperationForbidden)) {
      route
    }

  override def createTenant(seed: TenantSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route =
    authorize(ADMIN_ROLE, API_ROLE, M2M_ROLE, SECURITY_ROLE) {
      logger.info(s"Creating tenant with external id ${seed.externalId}")
      val result: Future[Tenant] = for {
        _      <- getUidFuture(contexts)
        tenant <- tenantManagementService.createTenant(seed.toDependency)
      } yield tenant.toApi

      onComplete(result) {
        handleApiError() orElse {
          case Success(tenant) =>
            createTenant201(tenant)
          case Failure(ex)     =>
            logger.error(s"Error creating tenant with external id ${seed.externalId}", ex)
            internalServerError()
        }
      }
    }

  override def getTenant(id: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = authorize(ADMIN_ROLE, API_ROLE, M2M_ROLE, SECURITY_ROLE) {
    logger.info(s"Retrieving tenant $id")
    val result: Future[Tenant] = for {
      uuid   <- id.toFutureUUID
      tenant <- tenantManagementService.getTenant(uuid)
    } yield tenant.toApi

    onComplete(result) {
      handleApiError() orElse {
        case Success(tenant) =>
          getTenant200(tenant)
        case Failure(ex)     =>
          logger.error(s"Error while retrieving tenant $id", ex)
          internalServerError()
      }
    }
  }

  def handleApiError()(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): PartialFunction[Try[_], StandardRoute] = { case Failure(err: TenantApiError[_]) =>
    logger.error("Error received from tenant Management - {}", err.responseContent)

    err.responseContent match {
      case Some(body: String) =>
        TenantProblem.fromString(body).fold(_ => internalServerError(), problem => complete(problem.status, problem))
      case _                  => internalServerError()
    }
  }

  def internalServerError()(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): StandardRoute = {
    val problem = problemOf(StatusCodes.InternalServerError, GenericError("Error while executing the request"))
    complete(problem.status, problem)
  }
}
