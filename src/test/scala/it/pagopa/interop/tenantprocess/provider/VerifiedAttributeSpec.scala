package it.pagopa.interop.tenantprocess.provider

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.agreementmanagement.client.model.AgreementState
import it.pagopa.interop.tenantmanagement.client.model.{TenantAttribute, TenantVerifier, VerifiedTenantAttribute}
import it.pagopa.interop.tenantprocess.api.adapters.ApiAdapters.VerificationRenewalWrapper
import it.pagopa.interop.tenantprocess.api.impl.TenantApiMarshallerImpl._
import it.pagopa.interop.tenantprocess.model.{VerificationRenewal, VerifiedTenantAttributeSeed}
import it.pagopa.interop.tenantprocess.utils.SpecHelper
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class VerifiedAttributeSpec extends AnyWordSpecLike with SpecHelper with ScalatestRouteTest {

  "Verified attribute verification" should {
    "add the attribute to the target Tenant when not present" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId        = UUID.randomUUID()
      val attributeId           = UUID.randomUUID()
      val tenant                = dependencyTenant.copy(
        id = targetTenantId,
        attributes = Seq(
          dependencyCertifiedTenantAttribute,
          dependencyDeclaredTenantAttribute,
          dependencyVerifiedTenantAttribute()
        )
      )
      val (agreement, eService) = matchingAgreementAndEService(attributeId)

      val seed = VerifiedTenantAttributeSeed(attributeId, VerificationRenewal.AUTOMATIC_RENEWAL, Some(timestamp))
      val managementSeed = TenantAttribute(
        declared = None,
        certified = None,
        verified = Some(
          VerifiedTenantAttribute(
            id = seed.id,
            assignmentTimestamp = timestamp,
            verifiedBy = Seq(
              TenantVerifier(
                id = organizationId,
                verificationDate = timestamp,
                renewal = seed.renewal.toDependency,
                expirationDate = seed.expirationDate,
                extensionDate = None
              )
            ),
            revokedBy = Nil
          )
        )
      )

      mockDateTimeGet()
      mockGetAgreements(organizationId, targetTenantId, Seq(AgreementState.PENDING), Seq(agreement))
      mockGetEServiceById(eService.id, eService)
      mockGetTenantById(targetTenantId, tenant)
      mockAddTenantAttribute(targetTenantId, managementSeed)
      mockComputeAgreementState(targetTenantId, attributeId)

      Post() ~> tenantService.verifyVerifiedAttribute(targetTenantId.toString, seed) ~> check {
        assert(status == StatusCodes.OK)
      }
    }

    "update the attribute of the target Tenant when present and add verifier" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId        = UUID.randomUUID()
      val attributeId           = UUID.randomUUID()
      val existingVerification  =
        dependencyVerifiedTenantAttribute(attributeId, assignmentTimestamp = timestamp.minusDays(1))
      val tenant                = dependencyTenant.copy(
        id = targetTenantId,
        attributes = Seq(dependencyCertifiedTenantAttribute, dependencyDeclaredTenantAttribute, existingVerification)
      )
      val (agreement, eService) = matchingAgreementAndEService(attributeId)

      val seed = VerifiedTenantAttributeSeed(attributeId, VerificationRenewal.AUTOMATIC_RENEWAL, Some(timestamp))
      val managementSeed = TenantAttribute(
        declared = None,
        certified = None,
        verified = Some(
          VerifiedTenantAttribute(
            id = seed.id,
            assignmentTimestamp = existingVerification.verified.get.assignmentTimestamp,
            verifiedBy = existingVerification.verified.get.verifiedBy :+
              TenantVerifier(
                id = organizationId,
                verificationDate = timestamp,
                renewal = seed.renewal.toDependency,
                expirationDate = seed.expirationDate,
                extensionDate = None
              ),
            revokedBy = existingVerification.verified.get.revokedBy
          )
        )
      )

      mockDateTimeGet()
      mockGetAgreements(organizationId, targetTenantId, Seq(AgreementState.PENDING), Seq(agreement))
      mockGetEServiceById(eService.id, eService)
      mockGetTenantById(targetTenantId, tenant)
      mockUpdateTenantAttribute(targetTenantId, seed.id, managementSeed)
      mockComputeAgreementState(targetTenantId, attributeId)

      Post() ~> tenantService.verifyVerifiedAttribute(targetTenantId.toString, seed) ~> check {
        assert(status == StatusCodes.OK)
      }
    }

    "update the attribute of the target Tenant adding verifier and keeping revoker when previously revoked" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId        = UUID.randomUUID()
      val attributeId           = UUID.randomUUID()
      val existingRevocation    = tenantRevoker.copy(id = organizationId, verificationDate = timestamp.minusDays(2))
      val existingVerification  =
        dependencyVerifiedTenantAttribute(
          attributeId,
          revokedBy = Seq(existingRevocation),
          assignmentTimestamp = timestamp.minusDays(1)
        )
      val tenant                = dependencyTenant.copy(
        id = targetTenantId,
        attributes = Seq(dependencyCertifiedTenantAttribute, dependencyDeclaredTenantAttribute, existingVerification)
      )
      val (agreement, eService) = matchingAgreementAndEService(attributeId)

      val seed = VerifiedTenantAttributeSeed(attributeId, VerificationRenewal.AUTOMATIC_RENEWAL, Some(timestamp))
      val managementSeed = TenantAttribute(
        declared = None,
        certified = None,
        verified = Some(
          VerifiedTenantAttribute(
            id = seed.id,
            assignmentTimestamp = existingVerification.verified.get.assignmentTimestamp,
            verifiedBy = existingVerification.verified.get.verifiedBy :+
              TenantVerifier(
                id = organizationId,
                verificationDate = timestamp,
                renewal = seed.renewal.toDependency,
                expirationDate = seed.expirationDate,
                extensionDate = None
              ),
            revokedBy = existingVerification.verified.get.revokedBy
          )
        )
      )

      mockDateTimeGet()
      mockGetAgreements(organizationId, targetTenantId, Seq(AgreementState.PENDING), Seq(agreement))
      mockGetEServiceById(eService.id, eService)
      mockGetTenantById(targetTenantId, tenant)
      mockUpdateTenantAttribute(targetTenantId, seed.id, managementSeed)
      mockComputeAgreementState(targetTenantId, attributeId)

      Post() ~> tenantService.verifyVerifiedAttribute(targetTenantId.toString, seed) ~> check {
        assert(status == StatusCodes.OK)
      }
    }

    "fail with 403 if Tenant is verifying own attribute" in {
      implicit val context: Seq[(String, String)] = adminContext

      val seed = VerifiedTenantAttributeSeed(UUID.randomUUID(), VerificationRenewal.AUTOMATIC_RENEWAL, None)

      mockDateTimeGet()

      Post() ~> tenantService.verifyVerifiedAttribute(organizationId.toString, seed) ~> check {
        assert(status == StatusCodes.Forbidden)
      }
    }

    "fail with 403 if requester is not a Producer of a Pending agreement containing the attribute" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId     = UUID.randomUUID()
      val attributeId        = UUID.randomUUID()
      val anotherAttributeId = UUID.randomUUID()

      val (agreement, eService) = matchingAgreementAndEService(anotherAttributeId)

      val seed = VerifiedTenantAttributeSeed(attributeId, VerificationRenewal.AUTOMATIC_RENEWAL, Some(timestamp))

      mockDateTimeGet()
      mockGetAgreements(organizationId, targetTenantId, Seq(AgreementState.PENDING), Seq(agreement))
      mockGetEServiceById(eService.id, eService)

      Post() ~> tenantService.verifyVerifiedAttribute(targetTenantId.toString, seed) ~> check {
        assert(status == StatusCodes.Forbidden)
      }
    }

    "fail with 409 if requester has already verified the attribute for target Tenant" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId = UUID.randomUUID()
      val attributeId    = UUID.randomUUID()

      val existingVerifier      = tenantVerifier.copy(id = organizationId, verificationDate = timestamp.minusDays(2))
      val existingVerification  =
        dependencyVerifiedTenantAttribute(
          attributeId,
          assignmentTimestamp = timestamp.minusDays(1),
          verifiedBy = Seq(existingVerifier)
        )
      val tenant                = dependencyTenant.copy(
        id = targetTenantId,
        attributes = Seq(dependencyCertifiedTenantAttribute, dependencyDeclaredTenantAttribute, existingVerification)
      )
      val (agreement, eService) = matchingAgreementAndEService(attributeId)

      val seed = VerifiedTenantAttributeSeed(attributeId, VerificationRenewal.AUTOMATIC_RENEWAL, Some(timestamp))

      mockDateTimeGet()
      mockGetAgreements(organizationId, targetTenantId, Seq(AgreementState.PENDING), Seq(agreement))
      mockGetEServiceById(eService.id, eService)
      mockGetTenantById(targetTenantId, tenant)

      Post() ~> tenantService.verifyVerifiedAttribute(targetTenantId.toString, seed) ~> check {
        assert(status == StatusCodes.Conflict)
      }
    }

  }

}
