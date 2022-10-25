package it.pagopa.interop.tenantprocess.provider

import cats.syntax.all._
import akka.http.scaladsl.model.StatusCodes
import it.pagopa.interop.tenantprocess.api.impl.TenantApiMarshallerImpl._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.tenantprocess.utils.SpecHelper
import org.scalatest.wordspec.AnyWordSpecLike
import it.pagopa.interop.tenantprocess.api.adapters.ApiAdapters._
import it.pagopa.interop.tenantprocess.api.adapters.TenantManagementAdapters._

import java.util.UUID
import it.pagopa.interop.tenantprocess.model.TenantDelta
import it.pagopa.interop.tenantprocess.model.Tenant
import it.pagopa.interop.tenantmanagement.client.model

class TenantUpdateSpec extends AnyWordSpecLike with SpecHelper with ScalatestRouteTest {

  "Tenant updated should not alter the tenant management behaviour" in {
    implicit val contexts: Seq[(String, String)] = adminContext

    val tenantId: UUID           = UUID.randomUUID()
    val tenantDelta: TenantDelta = TenantDelta(selfcareId = "paperino".some, features = Nil, mails = Nil)

    val tenantThatManagementReturns: model.Tenant = dependencyTenant
    val expected: Tenant                          = tenantThatManagementReturns.toApi

    mockUpdateTenant(tenantId, tenantDelta.fromAPI, tenantThatManagementReturns)

    Post() ~> tenantService.updateTenant(tenantId.toString(), tenantDelta) ~> check {
      assert(status == StatusCodes.OK)
      assert(entityAs[Tenant] == expected)
    }
  }
}
