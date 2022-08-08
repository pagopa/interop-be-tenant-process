package it.pagopa.interop.tenantprocess.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import it.pagopa.interop.commons.utils.extractHeaders
import it.pagopa.interop.tenantmanagement.client.invoker.BearerToken
import it.pagopa.interop.tenantmanagement.client.model._
import it.pagopa.interop.tenantprocess.service.{TenantManagementApi, TenantManagementInvoker, TenantManagementService}

import scala.concurrent.{ExecutionContext, Future}

final case class TenantManagementServiceImpl(invoker: TenantManagementInvoker, api: TenantManagementApi)(implicit
  ec: ExecutionContext
) extends TenantManagementService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def createTenant(seed: TenantSeed)(implicit contexts: Seq[(String, String)]): Future[Tenant] = for {
    (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
    request = api.createTenant(xCorrelationId = correlationId, seed, xForwardedFor = ip)(BearerToken(bearerToken))
    result <- invoker.invoke(
      request,
      s"Creating tenant for Origin ${seed.externalId.origin} and Value ${seed.externalId.value}"
    )
  } yield result

}
