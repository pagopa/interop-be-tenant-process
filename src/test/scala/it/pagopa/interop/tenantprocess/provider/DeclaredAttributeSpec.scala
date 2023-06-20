package it.pagopa.interop.tenantprocess.provider

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
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
      mockGetTenantById(organizationId, persistentTenant)
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

      val targetTenantId   = organizationId
      val attributeId      = UUID.randomUUID()
      val existingDeclared =
        persistentDeclaredAttribute.copy(id = attributeId, assignmentTimestamp = timestamp.minusDays(1))
      val tenant           = persistentTenant.copy(id = targetTenantId, attributes = List(existingDeclared))

      val managementSeed = Dependency.TenantAttribute(
        declared = Some(
          Dependency.DeclaredTenantAttribute(
            attributeId,
            assignmentTimestamp = timestamp.minusDays(1),
            revocationTimestamp = Some(timestamp)
          )
        ),
        certified = None,
        verified = None
      )

      mockDateTimeGet()
      mockGetTenantById(targetTenantId, tenant)
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

      val targetTenantId = organizationId
      val attributeId    = UUID.randomUUID()
      val seed           = DeclaredTenantAttributeSeed(attributeId)

      val existingDeclared =
        persistentDeclaredAttribute.copy(id = attributeId, assignmentTimestamp = timestamp.minusDays(1))
      val tenant           = persistentTenant.copy(id = targetTenantId, attributes = List(existingDeclared))

      val managementSeed = Dependency.TenantAttribute(
        declared = Some(
          Dependency.DeclaredTenantAttribute(
            seed.id,
            assignmentTimestamp = existingDeclared.assignmentTimestamp,
            revocationTimestamp = None
          )
        ),
        certified = None,
        verified = None
      )

      mockDateTimeGet()
      mockGetTenantById(targetTenantId, tenant)
      mockUpdateTenantAttribute(organizationId, attributeId, managementSeed)
      mockComputeAgreementState(organizationId, attributeId)

      Post() ~> tenantService.addDeclaredAttribute(seed) ~> check {
        assert(status == StatusCodes.OK)
      }
    }
  }
}
