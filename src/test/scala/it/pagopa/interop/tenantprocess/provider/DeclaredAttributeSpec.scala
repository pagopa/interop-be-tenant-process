package it.pagopa.interop.tenantprocess.provider

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.tenantmanagement.client.model.{DeclaredTenantAttribute, TenantAttribute}
import it.pagopa.interop.tenantprocess.model.DeclaredTenantAttributeSeed
import it.pagopa.interop.tenantprocess.utils.SpecHelper
import org.scalatest.wordspec.AnyWordSpecLike
import it.pagopa.interop.tenantprocess.api.impl.TenantApiMarshallerImpl._

import java.util.UUID

class DeclaredAttributeSpec extends AnyWordSpecLike with SpecHelper with ScalatestRouteTest {

  "Declared attribute addition" should {
    "succeed" in {
      implicit val context: Seq[(String, String)] = adminContext

      val attributeId    = UUID.randomUUID()
      val seed           = DeclaredTenantAttributeSeed(attributeId)
      val managementSeed = TenantAttribute(
        declared = Some(DeclaredTenantAttribute(seed.id, assignmentTimestamp = timestamp, revocationTimestamp = None)),
        certified = None,
        verified = None
      )

      mockDateTimeGet()
      mockAddTenantAttribute(organizationId, managementSeed)

      Post() ~> tenantService.addDeclaredAttribute(seed) ~> check {
        assert(status == StatusCodes.OK)
      }
    }
  }

  "Declared attribute revoke" should {
    "succeed" in {
      implicit val context: Seq[(String, String)] = adminContext

      val attribute   = dependencyDeclaredTenantAttribute
      val attributeId = attribute.declared.get.id

      val managementSeed = TenantAttribute(
        declared = Some(
          DeclaredTenantAttribute(attributeId, assignmentTimestamp = timestamp, revocationTimestamp = Some(timestamp))
        ),
        certified = None,
        verified = None
      )

      mockDateTimeGet()
      mockGetTenantAttribute(organizationId, attributeId, attribute)
      mockUpdateTenantAttribute(organizationId, attributeId, managementSeed)

      Post() ~> tenantService.revokeDeclaredAttribute(attributeId.toString) ~> check {
        assert(status == StatusCodes.OK)
      }
    }
  }
}
