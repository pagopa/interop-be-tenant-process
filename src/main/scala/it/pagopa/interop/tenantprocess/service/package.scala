package it.pagopa.interop.tenantprocess

import akka.actor.ActorSystem
import it.pagopa.interop.tenantmanagement

import scala.concurrent.ExecutionContextExecutor

package object service {
  type TenantManagementInvoker = tenantmanagement.client.invoker.ApiInvoker
  type TenantManagementApi     = tenantmanagement.client.api.TenantApi

  object TenantManagementInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): TenantManagementInvoker =
      tenantmanagement.client.invoker.ApiInvoker(tenantmanagement.client.api.EnumsSerializers.all, blockingEc)
  }

  object TenantManagementApi {
    def apply(baseUrl: String): TenantManagementApi = tenantmanagement.client.api.TenantApi(baseUrl)
  }

}
