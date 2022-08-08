package it.pagopa.interop.tenantprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.{Route, StandardRoute}
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.jwt.{ADMIN_ROLE, authorizeInterop, hasPermissions}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils.getUidFuture
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.OperationForbidden
import it.pagopa.interop.commons.utils.service.{OffsetDateTimeSupplier, UUIDSupplier}
import it.pagopa.interop.tenantmanagement.client.invoker.{ApiError => TenantApiError}
import it.pagopa.interop.tenantmanagement.client.model.{Problem => TenantProblem}
import it.pagopa.interop.tenantprocess.api.TenantApiService
import it.pagopa.interop.tenantprocess.api.adapters.ApiAdapters._
import it.pagopa.interop.tenantprocess.api.adapters.TenantManagementAdapters._
import it.pagopa.interop.tenantprocess.error.TenantProcessErrors.CreateTenantBadRequest
import it.pagopa.interop.tenantprocess.model._
import it.pagopa.interop.tenantprocess.service._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

final case class TenantApiServiceImpl(
  tenantManagementService: TenantManagementService,
  uuidSupplier: UUIDSupplier,
  dateTimeSupplier: OffsetDateTimeSupplier
)(implicit ec: ExecutionContext)
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
    authorize(ADMIN_ROLE) {
      logger.info("Creating tenant {}", seed)
      val result: Future[Tenant] = for {
        _      <- getUidFuture(contexts)
        tenant <- tenantManagementService.createTenant(seed.toDependency)
      } yield tenant.toApi

      val defaultProblem: Problem = problemOf(StatusCodes.BadRequest, CreateTenantBadRequest)
      onComplete(result) {
        handleApiError(defaultProblem) orElse {
          case Success(tenant) =>
            createTenant201(tenant)
          case Failure(ex)     =>
            logger.error(s"Error creating tenant $seed ", ex)
            createTenant400(defaultProblem)
        }
      }
    }

  override def getTenant(id: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = ???
//  authorize(ADMIN_ROLE, M2M_ROLE) {
//    logger.info("Retrieving tenant {}", id)
//    val result: Future[tenant] = for {
//      userId   <- getUidFuture(contexts)
//      userUUID <- userId.toFutureUUID
//      uuid     <- id.toFutureUUID
//      tenant   <- tenantManagementService.gettenant(uuid)
//      userType <- userType(userUUID, tenant.eserviceId, tenant.consumerId)
//      result   <- enhancetenant(tenant, userType)
//    } yield result
//
//    val defaultProblem: Problem = problemOf(StatusCodes.BadRequest, GettenantBadRequest(id))
//    onComplete(result) {
//      handleApiError(defaultProblem) orElse handleUserTypeError orElse {
//        case Success(tenant) =>
//          gettenant200(tenant)
//        case Failure(ex)     =>
//          logger.error(s"Error while retrieving tenant $id", ex)
//          gettenant400(defaultProblem)
//      }
//    }
//  }

  def handleApiError(defaultProblem: Problem)(implicit
    contexts: Seq[(String, String)]
  ): PartialFunction[Try[_], StandardRoute] = { case Failure(err: TenantApiError[_]) =>
    logger.error("Error received from tenant Management - {}", err.responseContent)
    val problem = err.responseContent match {
      case Some(body: String) => TenantProblem.fromString(body).getOrElse(defaultProblem)
      case _                  => defaultProblem
    }
    complete(problem.status, problem)
  }
}
