package it.pagopa.interop.tenantprocess.service.impl

import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.agreementmanagement.client.invoker.{ApiRequest, BearerToken}
import it.pagopa.interop.agreementmanagement.client.model.{Agreement, AgreementState}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.withHeaders
import it.pagopa.interop.tenantprocess.service.{
  AgreementManagementApi,
  AgreementManagementInvoker,
  AgreementManagementService
}

import java.util.UUID
import scala.concurrent.Future

final case class AgreementManagementServiceImpl(invoker: AgreementManagementInvoker, api: AgreementManagementApi)
    extends AgreementManagementService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getAgreements(producerId: UUID, consumerId: UUID, states: Seq[AgreementState])(implicit
    contexts: Seq[(String, String)]
  ): Future[Seq[Agreement]] = withHeaders { (bearerToken, correlationId, ip) =>
    val request: ApiRequest[Seq[Agreement]] =
      api.getAgreements(
        xCorrelationId = correlationId,
        producerId = producerId.toString.some,
        consumerId = consumerId.toString.some,
        states = states,
        xForwardedFor = ip
      )(BearerToken(bearerToken))
    invoker.invoke(
      request,
      s"Retrieve Agreements for producer $producerId, consumer $consumerId and states ${states.mkString(",")}"
    )
  }
}
