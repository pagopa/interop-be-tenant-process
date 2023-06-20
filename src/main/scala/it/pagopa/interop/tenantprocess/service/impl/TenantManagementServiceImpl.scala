package it.pagopa.interop.tenantprocess.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.withHeaders
import it.pagopa.interop.tenantmanagement.client.invoker.{ApiError, BearerToken}
import it.pagopa.interop.tenantmanagement.client.model._
import it.pagopa.interop.tenantprocess.error.TenantProcessErrors.TenantByIdNotFound
import it.pagopa.interop.tenantprocess.service.{
  TenantManagementApi,
  TenantManagementAttributesApi,
  TenantManagementInvoker,
  TenantManagementService
}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class TenantManagementServiceImpl(
  invoker: TenantManagementInvoker,
  tenantApi: TenantManagementApi,
  attributeApi: TenantManagementAttributesApi
)(implicit ec: ExecutionContext)
    extends TenantManagementService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def createTenant(seed: TenantSeed)(implicit contexts: Seq[(String, String)]): Future[Tenant] = withHeaders {
    (bearerToken, correlationId, ip) =>
      val request =
        tenantApi.createTenant(xCorrelationId = correlationId, seed, xForwardedFor = ip)(BearerToken(bearerToken))
      invoker.invoke(
        request,
        s"Creating tenant for Origin ${seed.externalId.origin} and Value ${seed.externalId.value}"
      )
  }

  override def updateTenant(tenantId: UUID, payload: TenantDelta)(implicit
    contexts: Seq[(String, String)]
  ): Future[Tenant] = withHeaders { (bearerToken, correlationId, ip) =>
    val request = tenantApi.updateTenant(
      xCorrelationId = correlationId,
      tenantId = tenantId,
      tenantDelta = payload,
      xForwardedFor = ip
    )(BearerToken(bearerToken))
    invoker
      .invoke(request, s"Updating tenant with id $tenantId")
      .recoverWith {
        case err: ApiError[_] if err.code == 404 =>
          Future.failed(TenantByIdNotFound(tenantId))
      }
  }

  override def addTenantAttribute(tenantId: UUID, attribute: TenantAttribute)(implicit
    contexts: Seq[(String, String)]
  ): Future[Tenant] = withHeaders { (bearerToken, correlationId, ip) =>
    val request = attributeApi.addTenantAttribute(
      xCorrelationId = correlationId,
      tenantId = tenantId,
      tenantAttribute = attribute,
      xForwardedFor = ip
    )(BearerToken(bearerToken))
    val id      = attribute.certified
      .map(_.id)
      .orElse(attribute.verified.map(_.id))
      .orElse(attribute.declared.map(_.id))
      .map(_.toString())
      .getOrElse("[UNKNOWN]")
    invoker.invoke(request, s"Adding attribute $id to tenant $tenantId")
  }

  override def updateTenantAttribute(tenantId: UUID, attributeId: UUID, attribute: TenantAttribute)(implicit
    contexts: Seq[(String, String)]
  ): Future[Tenant] = withHeaders { (bearerToken, correlationId, ip) =>
    val request = attributeApi.updateTenantAttribute(
      xCorrelationId = correlationId,
      tenantId = tenantId,
      attributeId = attributeId,
      tenantAttribute = attribute,
      xForwardedFor = ip
    )(BearerToken(bearerToken))
    invoker.invoke(request, s"Deleting attribute $attributeId from tenant $tenantId")
  }
}
