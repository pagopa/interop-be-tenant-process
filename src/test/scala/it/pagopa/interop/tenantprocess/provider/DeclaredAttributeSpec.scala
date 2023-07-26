package it.pagopa.interop.tenantprocess.provider

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.agreementprocess.client.model.CompactTenant
import it.pagopa.interop.tenantmanagement.client.{model => Dependency}
import it.pagopa.interop.tenantprocess.model.DeclaredTenantAttributeSeed
import it.pagopa.interop.tenantprocess.api.impl.TenantApiMarshallerImpl._
import it.pagopa.interop.tenantprocess.utils.SpecHelper
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class DeclaredAttributeSpec extends AnyWordSpecLike with SpecHelper with ScalatestRouteTest {

  "Declared attribute addition" should {
    "succeed" in {
      implicit val context: Seq[(String, String)] = adminContext

      val attributeId    = UUID.randomUUID()
      val seed           = DeclaredTenantAttributeSeed(attributeId)
      val managementSeed = Dependency.TenantAttribute(
        declared = Some(
          Dependency.DeclaredTenantAttribute(seed.id, assignmentTimestamp = timestamp, revocationTimestamp = None)
        ),
        certified = None,
        verified = None
      )

      mockDateTimeGet()
      mockGetTenantAttributeNotFound(organizationId, attributeId)
      mockAddTenantAttribute(organizationId, managementSeed)
      mockComputeAgreementState(attributeId, CompactTenant(organizationId, Nil))

      Post() ~> tenantService.addDeclaredAttribute(seed) ~> check {
        assert(status == StatusCodes.OK)
      }
    }
  }

  "Declared attribute revoke" should {
    "succeed" in {
      implicit val context: Seq[(String, String)] = adminContext

      val attribute = persistentDeclaredAttribute

      val managementSeed = Dependency.TenantAttribute(
        declared = Some(
          Dependency.DeclaredTenantAttribute(
            attribute.id,
            assignmentTimestamp = timestamp,
            revocationTimestamp = Some(timestamp)
          )
        ),
        certified = None,
        verified = None
      )

      mockDateTimeGet()
      mockGetTenantAttribute(organizationId, attribute.id, attribute)
      mockUpdateTenantAttribute(organizationId, attribute.id, managementSeed)
      mockComputeAgreementState(attribute.id, CompactTenant(organizationId, Nil))

      Post() ~> tenantService.revokeDeclaredAttribute(attribute.id.toString) ~> check {
        assert(status == StatusCodes.OK)
      }
    }
  }

  "Declared attribute re-assignment" should {
    "succeed" in {
      implicit val context: Seq[(String, String)] = adminContext

      val attributeId = UUID.randomUUID()
      val seed        = DeclaredTenantAttributeSeed(attributeId)

      val existingAttribute =
        persistentDeclaredAttribute.copy(id = attributeId, revocationTimestamp = Some(timestamp))

      val managementSeed = Dependency.TenantAttribute(
        declared = Some(
          Dependency.DeclaredTenantAttribute(
            seed.id,
            assignmentTimestamp = existingAttribute.assignmentTimestamp,
            revocationTimestamp = None
          )
        ),
        certified = None,
        verified = None
      )

      mockDateTimeGet()
      mockGetTenantAttribute(organizationId, attributeId, existingAttribute)
      mockUpdateTenantAttribute(organizationId, attributeId, managementSeed)
      mockComputeAgreementState(attributeId, CompactTenant(organizationId, Nil))

      Post() ~> tenantService.addDeclaredAttribute(seed) ~> check {
        assert(status == StatusCodes.OK)
      }
    }
  }
}
