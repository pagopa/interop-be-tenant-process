package it.pagopa.interop.tenantprocess

import akka.actor.ActorSystem
import it.pagopa.interop._

import scala.concurrent.ExecutionContextExecutor

package object service {
  type AttributeRegistryManagementInvoker = attributeregistrymanagement.client.invoker.ApiInvoker
  type AttributeRegistryManagementApi     = attributeregistrymanagement.client.api.AttributeApi

  type TenantManagementInvoker = tenantmanagement.client.invoker.ApiInvoker
  type TenantManagementApi     = tenantmanagement.client.api.TenantApi

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

}
