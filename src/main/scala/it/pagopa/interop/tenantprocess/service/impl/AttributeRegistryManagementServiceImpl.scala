package it.pagopa.interop.tenantprocess.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.attributeregistrymanagement.client.invoker.BearerToken
import it.pagopa.interop.attributeregistrymanagement.client.model.Attribute
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import it.pagopa.interop.commons.utils.extractHeaders
import it.pagopa.interop.tenantprocess.service.{
  AttributeRegistryManagementApi,
  AttributeRegistryManagementInvoker,
  AttributeRegistryManagementService
}

import scala.concurrent.{ExecutionContext, Future}

final case class AttributeRegistryManagementServiceImpl(
  invoker: AttributeRegistryManagementInvoker,
  api: AttributeRegistryManagementApi
)(implicit ec: ExecutionContext)
    extends AttributeRegistryManagementService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getAttributeByExternalCode(origin: String, code: String)(implicit
    contexts: Seq[(String, String)]
  ): Future[Attribute] = for {
    (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
    request = api.getAttributeByOriginAndCode(
      xCorrelationId = correlationId,
      origin = origin,
      code = code,
      xForwardedFor = ip
    )(BearerToken(bearerToken))
    result <- invoker.invoke(request, s"Retrieve Attribute for Origin $origin and Code $code")
  } yield result

}
