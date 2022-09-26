package it.pagopa.interop.tenantprocess.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import it.pagopa.interop.commons.utils.extractHeaders
import it.pagopa.interop.tenantmanagement.client.invoker.BearerToken
import it.pagopa.interop.tenantmanagement.client.model._
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

  override def createTenant(seed: TenantSeed)(implicit contexts: Seq[(String, String)]): Future[Tenant] = for {
    (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
    request = tenantApi.createTenant(xCorrelationId = correlationId, seed, xForwardedFor = ip)(BearerToken(bearerToken))
    result <- invoker.invoke(
      request,
      s"Creating tenant for Origin ${seed.externalId.origin} and Value ${seed.externalId.value}"
    )
  } yield result

  override def getTenant(tenantId: UUID)(implicit contexts: Seq[(String, String)]): Future[Tenant] = for {
    (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
    request = tenantApi.getTenant(xCorrelationId = correlationId, tenantId = tenantId, xForwardedFor = ip)(
      BearerToken(bearerToken)
    )
    result <- invoker.invoke(request, s"Retrieving tenant with id $tenantId")
  } yield result

  override def updateTenant(tenantId: UUID, payload: TenantDelta)(implicit
    contexts: Seq[(String, String)]
  ): Future[Tenant] = for {
    (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
    request = tenantApi.updateTenant(
      xCorrelationId = correlationId,
      tenantId = tenantId,
      tenantDelta = payload,
      xForwardedFor = ip
    )(BearerToken(bearerToken))
    result <- invoker.invoke(request, s"Updating tenant with id $tenantId")
  } yield result

  override def addTenantAttribute(tenantId: UUID, attribute: TenantAttribute)(implicit
    contexts: Seq[(String, String)]
  ): Future[Tenant] = for {
    (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
    request = attributeApi.addTenantAttribute(
      xCorrelationId = correlationId,
      tenantId = tenantId,
      tenantAttribute = attribute,
      xForwardedFor = ip
    )(BearerToken(bearerToken))
    id      = attribute.certified
      .map(_.id)
      .orElse(attribute.verified.map(_.id))
      .orElse(attribute.declared.map(_.id))
      .map(_.toString())
      .getOrElse("")
    result <- invoker.invoke(request, s"Adding attribute $id to tenant $tenantId")
  } yield result

  override def updateTenantAttribute(tenantId: UUID, attributeId: UUID, attribute: TenantAttribute)(implicit
    contexts: Seq[(String, String)]
  ): Future[Tenant] = for {
    (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
    request = attributeApi.updateTenantAttribute(
      xCorrelationId = correlationId,
      tenantId = tenantId,
      attributeId = attributeId,
      tenantAttribute = attribute,
      xForwardedFor = ip
    )(BearerToken(bearerToken))
    result <- invoker.invoke(request, s"Deleting attribute $attributeId from tenant $tenantId")
  } yield result

  override def getTenantByExternalId(externalId: ExternalId)(implicit contexts: Seq[(String, String)]): Future[Tenant] =
    for {
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = tenantApi.getTenantByExternalId(
        xCorrelationId = correlationId,
        origin = externalId.origin,
        code = externalId.value,
        xForwardedFor = ip
      )(BearerToken(bearerToken))
      result <- invoker.invoke(
        request,
        s"Retrieving tenant with origin ${externalId.origin} and code ${externalId.value}"
      )
    } yield result

  override def getTenantAttribute(tenantId: UUID, attributeId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[TenantAttribute] = for {
    (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
    request = attributeApi.getTenantAttribute(
      xCorrelationId = correlationId,
      tenantId = tenantId,
      attributeId = attributeId,
      xForwardedFor = ip
    )(BearerToken(bearerToken))
    result <- invoker.invoke(request, s"Retrieving attribute $attributeId for tenant $tenantId")
  } yield result
}
