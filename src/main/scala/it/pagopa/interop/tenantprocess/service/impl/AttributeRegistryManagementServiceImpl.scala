package it.pagopa.interop.tenantprocess.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.attributeregistrymanagement.client.invoker.BearerToken
import it.pagopa.interop.attributeregistrymanagement.client.model.Attribute
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.withHeaders
import it.pagopa.interop.tenantprocess.service.{
  AttributeRegistryManagementApi,
  AttributeRegistryManagementInvoker,
  AttributeRegistryManagementService
}

import scala.concurrent.Future
import java.util.UUID
import it.pagopa.interop.attributeregistrymanagement.client.invoker.ApiRequest

final case class AttributeRegistryManagementServiceImpl(
  invoker: AttributeRegistryManagementInvoker,
  api: AttributeRegistryManagementApi
) extends AttributeRegistryManagementService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getAttributeByExternalCode(origin: String, code: String)(implicit
    contexts: Seq[(String, String)]
  ): Future[Attribute] = withHeaders { (bearerToken, correlationId, ip) =>
    val request: ApiRequest[Attribute] =
      api.getAttributeByOriginAndCode(xCorrelationId = correlationId, origin = origin, code = code, xForwardedFor = ip)(
        BearerToken(bearerToken)
      )
    invoker.invoke(request, s"Retrieve Attribute for Origin $origin and Code $code")
  }

  override def getAttributeById(id: UUID)(implicit contexts: Seq[(String, String)]): Future[Attribute] = withHeaders {
    (bearerToken, correlationId, ip) =>
      val request: ApiRequest[Attribute] =
        api.getAttributeById(xCorrelationId = correlationId, attributeId = id, xForwardedFor = ip)(
          BearerToken(bearerToken)
        )
      invoker.invoke(request, s"Retrieve Attribute $id")
  }

}
