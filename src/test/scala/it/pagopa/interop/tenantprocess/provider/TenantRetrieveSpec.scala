package it.pagopa.interop.tenantprocess.provider

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.tenantprocess.api.impl.TenantApiMarshallerImpl._
import it.pagopa.interop.tenantprocess.utils.SpecHelper
import org.scalatest.wordspec.AnyWordSpecLike
import java.util.UUID

class TenantRetrieveSpec extends AnyWordSpecLike with SpecHelper with ScalatestRouteTest {

  "Retrieve Tenants" should {

    "List producers" in {

      implicit val context: Seq[(String, String)] = adminContext

      val offset: Int          = 0
      val limit: Int           = 50
      val name: Option[String] = Some("name")

      mockGetProducers(name, offset, limit)

      Get() ~> tenantService.getProducers(name, offset, limit) ~> check {
        assert(status == StatusCodes.OK)
      }
    }

    "List consumers" in {

      implicit val context: Seq[(String, String)] = adminContext

      val producerId           = organizationId
      val offset: Int          = 0
      val limit: Int           = 50
      val name: Option[String] = Some("name")

      mockGetConsumers(name, producerId, offset, limit)

      Get() ~> tenantService.getConsumers(name, offset, limit) ~> check {
        assert(status == StatusCodes.OK)
      }
    }

    "Get Tenant by Id" in {

      implicit val context: Seq[(String, String)] = adminContext

      val tenantId = UUID.randomUUID

      mockGetTenantById(tenantId)

      Get() ~> tenantService.getTenant(tenantId.toString) ~> check {
        assert(status == StatusCodes.OK)
      }
    }
  }
}
