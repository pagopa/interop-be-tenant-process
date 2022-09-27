package it.pagopa.interop.tenantprocess.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.agreementprocess.client.invoker.BearerToken
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import it.pagopa.interop.commons.utils.extractHeaders
import it.pagopa.interop.tenantprocess.service.{AgreementProcessApi, AgreementProcessInvoker, AgreementProcessService}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class AgreementProcessServiceImpl(invoker: AgreementProcessInvoker, agreementApi: AgreementProcessApi)(
  implicit ec: ExecutionContext
) extends AgreementProcessService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def computeAgreementsByAttribute(consumerId: UUID, attributeId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit] = for {
    (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
    request = agreementApi.computeAgreementsByAttribute(
      xCorrelationId = correlationId,
      consumerId = consumerId,
      attributeId = attributeId,
      xForwardedFor = ip
    )(BearerToken(bearerToken))
    result <- invoker.invoke(
      request,
      s"Agreements state compute triggered for Tenant $consumerId and Attribute $attributeId"
    )
  } yield result
}
