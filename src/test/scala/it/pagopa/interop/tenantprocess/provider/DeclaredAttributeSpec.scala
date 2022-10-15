package it.pagopa.interop.tenantprocess.provider

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.implicits.catsSyntaxOptionId
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
      mockGetTenantAttributeNotFound(organizationId, attributeId)
      mockAddTenantAttribute(organizationId, managementSeed)
      mockComputeAgreementState(organizationId, attributeId)

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
      mockComputeAgreementState(organizationId, attributeId)

      Post() ~> tenantService.revokeDeclaredAttribute(attributeId.toString) ~> check {
        assert(status == StatusCodes.OK)
      }
    }
  }

  "Declared attribute re-assignment" should {
    "succeed" in {
      implicit val context: Seq[(String, String)] = adminContext

      val attributeId = UUID.randomUUID()
      val seed        = DeclaredTenantAttributeSeed(attributeId)

      val existingAttribute = dependencyDeclaredTenantAttribute.copy(declared =
        dependencyDeclaredTenantAttribute.declared.get.copy(id = attributeId, revocationTimestamp = timestamp.some).some
      )

      val managementSeed = TenantAttribute(
        declared = Some(
          DeclaredTenantAttribute(
            seed.id,
            assignmentTimestamp = existingAttribute.declared.get.assignmentTimestamp,
            revocationTimestamp = None
          )
        ),
        certified = None,
        verified = None
      )

      mockDateTimeGet()
      mockGetTenantAttribute(organizationId, attributeId, existingAttribute)
      mockUpdateTenantAttribute(organizationId, attributeId, managementSeed)
      mockComputeAgreementState(organizationId, attributeId)

      Post() ~> tenantService.addDeclaredAttribute(seed) ~> check {
        assert(status == StatusCodes.OK)
      }
    }
  }
}
