package it.pagopa.interop.tenantprocess

import akka.actor.ActorSystem
import it.pagopa.interop._

import scala.concurrent.ExecutionContextExecutor

package object service {
  type AttributeRegistryManagementInvoker = attributeregistrymanagement.client.invoker.ApiInvoker
  type AttributeRegistryManagementApi     = attributeregistrymanagement.client.api.AttributeApi

  type TenantManagementInvoker       = tenantmanagement.client.invoker.ApiInvoker
  type TenantManagementApi           = tenantmanagement.client.api.TenantApi
  type TenantManagementAttributesApi = tenantmanagement.client.api.AttributesApi

  type AgreementProcessInvoker    = agreementprocess.client.invoker.ApiInvoker
  type AgreementProcessApi        = agreementprocess.client.api.AgreementApi
  type AgreementManagementInvoker = agreementmanagement.client.invoker.ApiInvoker
  type AgreementManagementApi     = agreementmanagement.client.api.AgreementApi

  type CatalogManagementInvoker = catalogmanagement.client.invoker.ApiInvoker
  type CatalogManagementApi     = catalogmanagement.client.api.EServiceApi

  object AttributeRegistryManagementInvoker {
    def apply(
      blockingEc: ExecutionContextExecutor
    )(implicit actorSystem: ActorSystem): AttributeRegistryManagementInvoker =
      attributeregistrymanagement.client.invoker
        .ApiInvoker(attributeregistrymanagement.client.api.EnumsSerializers.all, blockingEc)
  }

  object AttributeRegistryManagementApi {
    def apply(baseUrl: String): AttributeRegistryManagementApi =
      attributeregistrymanagement.client.api.AttributeApi(baseUrl)
  }

  object TenantManagementInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): TenantManagementInvoker =
      tenantmanagement.client.invoker.ApiInvoker(tenantmanagement.client.api.EnumsSerializers.all, blockingEc)
  }

  object TenantManagementApi {
    def apply(baseUrl: String): TenantManagementApi = tenantmanagement.client.api.TenantApi(baseUrl)
  }

  object TenantManagementAttributesApi {
    def apply(baseUrl: String): TenantManagementAttributesApi = tenantmanagement.client.api.AttributesApi(baseUrl)
  }

  object AgreementProcessInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): AgreementProcessInvoker =
      agreementprocess.client.invoker.ApiInvoker(agreementprocess.client.api.EnumsSerializers.all, blockingEc)
  }

  object AgreementProcessApi {
    def apply(baseUrl: String): AgreementProcessApi = agreementprocess.client.api.AgreementApi(baseUrl)
  }

  object AgreementManagementInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): AgreementManagementInvoker =
      agreementmanagement.client.invoker.ApiInvoker(agreementmanagement.client.api.EnumsSerializers.all, blockingEc)
  }

  object AgreementManagementApi {
    def apply(baseUrl: String): AgreementManagementApi = agreementmanagement.client.api.AgreementApi(baseUrl)
  }

  object CatalogManagementInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): CatalogManagementInvoker =
      catalogmanagement.client.invoker.ApiInvoker(catalogmanagement.client.api.EnumsSerializers.all, blockingEc)
  }

  object CatalogManagementApi {
    def apply(baseUrl: String): CatalogManagementApi = catalogmanagement.client.api.EServiceApi(baseUrl)
  }

}
