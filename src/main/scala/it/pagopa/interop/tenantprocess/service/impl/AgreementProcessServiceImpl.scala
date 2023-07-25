package it.pagopa.interop.tenantprocess.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.agreementprocess.client.invoker.BearerToken
import it.pagopa.interop.agreementprocess.client.model.{CompactTenant, ComputeAgreementStatePayload}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import it.pagopa.interop.commons.utils.extractHeaders
import it.pagopa.interop.tenantprocess.service.{AgreementProcessApi, AgreementProcessInvoker, AgreementProcessService}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

// Note: The service takes a blocking execution context in order to implement a fire and forget call of computeAgreementsByAttribute
final case class AgreementProcessServiceImpl(
  invoker: AgreementProcessInvoker,
  agreementApi: AgreementProcessApi,
  blockingEc: ExecutionContext
) extends AgreementProcessService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def computeAgreementsByAttribute(attributeId: UUID, consumer: CompactTenant)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit] = {
    implicit val ec: ExecutionContext = blockingEc
    for {
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = agreementApi.computeAgreementsByAttribute(
        xCorrelationId = correlationId,
        computeAgreementStatePayload = ComputeAgreementStatePayload(attributeId, consumer),
        xForwardedFor = ip
      )(BearerToken(bearerToken))
      result <- invoker
        .invoke(request, s"Agreements state compute triggered for Tenant ${consumer.id} and Attribute $attributeId")
        .recoverWith { case _ => Future.unit } // Do not fail because this service should not be blocked by this update
    } yield result
  }
}
