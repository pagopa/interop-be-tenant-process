package it.pagopa.interop.tenantprocess.provider

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.agreementmanagement.client.model.AgreementState
import it.pagopa.interop.tenantmanagement.client.model.{
  TenantAttribute,
  TenantRevoker,
  TenantVerifier,
  VerifiedTenantAttribute
}
import it.pagopa.interop.tenantprocess.api.adapters.ApiAdapters.VerificationRenewalWrapper
import it.pagopa.interop.tenantprocess.api.impl.TenantApiMarshallerImpl._
import it.pagopa.interop.tenantprocess.model.{
  VerificationRenewal,
  VerifiedTenantAttributeSeed,
  UpdateVerifiedTenantAttributeSeed
}
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
                extensionDate = seed.expirationDate
              )
            ),
            revokedBy = Nil
          )
        )
      )

      mockDateTimeGet()
      mockGetAgreements(
        organizationId,
        targetTenantId,
        Seq(AgreementState.PENDING, AgreementState.ACTIVE, AgreementState.SUSPENDED),
        Seq(agreement)
      )
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
      mockGetAgreements(
        organizationId,
        targetTenantId,
        Seq(AgreementState.PENDING, AgreementState.ACTIVE, AgreementState.SUSPENDED),
        Seq(agreement)
      )
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
      mockGetAgreements(
        organizationId,
        targetTenantId,
        Seq(AgreementState.PENDING, AgreementState.ACTIVE, AgreementState.SUSPENDED),
        Seq(agreement)
      )
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
      mockGetAgreements(
        organizationId,
        targetTenantId,
        Seq(AgreementState.PENDING, AgreementState.ACTIVE, AgreementState.SUSPENDED),
        Seq(agreement)
      )
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
      mockGetAgreements(
        organizationId,
        targetTenantId,
        Seq(AgreementState.PENDING, AgreementState.ACTIVE, AgreementState.SUSPENDED),
        Seq(agreement)
      )
      mockGetEServiceById(eService.id, eService)
      mockGetTenantById(targetTenantId, tenant)
      mockUpdateTenantAttribute(targetTenantId, attributeId, managementSeed)
      mockComputeAgreementState(targetTenantId, attributeId)

      Post() ~> tenantService.verifyVerifiedAttribute(targetTenantId.toString, seed) ~> check {
        assert(status == StatusCodes.OK)
      }
    }
  }

  "Verified attribute update strategy" should {
    "succeed with new renewal and expirationDate" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId = UUID.randomUUID()
      val attributeId    = UUID.randomUUID()

      val existingVerifier     = tenantVerifier.copy(id = organizationId, verificationDate = timestamp.minusDays(2))
      val existingVerification =
        dependencyVerifiedTenantAttribute(
          attributeId,
          assignmentTimestamp = timestamp.minusDays(1),
          verifiedBy = Seq(existingVerifier)
        )
      val tenant               = dependencyTenant.copy(
        id = targetTenantId,
        attributes = Seq(dependencyCertifiedTenantAttribute, dependencyDeclaredTenantAttribute, existingVerification)
      )

      val newExpirationDate = Some(timestamp.plusDays(10))

      val seed =
        UpdateVerifiedTenantAttributeSeed(Some(VerificationRenewal.AUTOMATIC_RENEWAL), newExpirationDate)

      val managementSeed = TenantAttribute(
        declared = None,
        certified = None,
        verified = Some(
          VerifiedTenantAttribute(
            id = attributeId,
            assignmentTimestamp = existingVerification.verified.get.assignmentTimestamp,
            verifiedBy = existingVerification.verified.get.verifiedBy.filterNot(_.id == organizationId) :+
              TenantVerifier(
                id = organizationId,
                verificationDate = timestamp,
                renewal = VerificationRenewal.AUTOMATIC_RENEWAL.toDependency,
                expirationDate = newExpirationDate,
                extensionDate = None
              ),
            revokedBy = existingVerification.verified.get.revokedBy
          )
        )
      )

      mockDateTimeGet()

      mockGetTenantById(targetTenantId, tenant)
      mockUpdateTenantAttribute(targetTenantId, attributeId, managementSeed)

      Post() ~> tenantService.updateVerifiedAttribute(targetTenantId.toString, attributeId.toString, seed) ~> check {
        assert(status == StatusCodes.OK)
      }
    }

    "succeed with new expirationDate and extensionDate" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId = UUID.randomUUID()
      val attributeId    = UUID.randomUUID()

      val existingVerifier     = tenantVerifier.copy(id = organizationId, verificationDate = timestamp.minusDays(2))
      val existingVerification =
        dependencyVerifiedTenantAttribute(
          attributeId,
          assignmentTimestamp = timestamp.minusDays(1),
          verifiedBy = Seq(existingVerifier)
        )
      val tenant               = dependencyTenant.copy(
        id = targetTenantId,
        attributes = Seq(dependencyCertifiedTenantAttribute, dependencyDeclaredTenantAttribute, existingVerification)
      )

      val (newExpirationDate, newExtensionDate) = (Some(timestamp.plusDays(10)), Some(timestamp.plusDays(30)))

      val seed =
        UpdateVerifiedTenantAttributeSeed(None, newExpirationDate, newExtensionDate)

      val managementSeed = TenantAttribute(
        declared = None,
        certified = None,
        verified = Some(
          VerifiedTenantAttribute(
            id = attributeId,
            assignmentTimestamp = existingVerification.verified.get.assignmentTimestamp,
            verifiedBy = existingVerification.verified.get.verifiedBy.filterNot(_.id == organizationId) :+
              TenantVerifier(
                id = organizationId,
                verificationDate = timestamp,
                renewal = existingVerifier.renewal,
                expirationDate = newExpirationDate,
                extensionDate = newExtensionDate
              ),
            revokedBy = existingVerification.verified.get.revokedBy
          )
        )
      )

      mockDateTimeGet()

      mockGetTenantById(targetTenantId, tenant)
      mockUpdateTenantAttribute(targetTenantId, attributeId, managementSeed)

      Post() ~> tenantService.updateVerifiedAttribute(targetTenantId.toString, attributeId.toString, seed) ~> check {
        assert(status == StatusCodes.OK)
      }
    }

    "fail if expiration date is in the past" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId = UUID.randomUUID()
      val attributeId    = UUID.randomUUID()

      val seed =
        UpdateVerifiedTenantAttributeSeed(Some(VerificationRenewal.AUTOMATIC_RENEWAL), Some(timestamp.minusDays(2)))

      mockDateTimeGet()

      Post() ~> tenantService.updateVerifiedAttribute(targetTenantId.toString, attributeId.toString, seed) ~> check {
        assert(status == StatusCodes.BadRequest)
      }
    }

    "fail if Requester is not a previous verifier of verified attribute" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId = UUID.randomUUID()
      val attributeId    = UUID.randomUUID()

      val existingVerifier     = tenantVerifier.copy(id = UUID.randomUUID(), verificationDate = timestamp.minusDays(2))
      val existingVerification =
        dependencyVerifiedTenantAttribute(
          attributeId,
          assignmentTimestamp = timestamp.minusDays(1),
          verifiedBy = Seq(existingVerifier)
        )
      val tenant               = dependencyTenant.copy(
        id = targetTenantId,
        attributes = Seq(dependencyCertifiedTenantAttribute, dependencyDeclaredTenantAttribute, existingVerification)
      )

      val seed =
        UpdateVerifiedTenantAttributeSeed(Some(VerificationRenewal.AUTOMATIC_RENEWAL), Some(timestamp.plusDays(10)))

      mockDateTimeGet()

      mockGetTenantById(targetTenantId, tenant)

      Post() ~> tenantService.updateVerifiedAttribute(targetTenantId.toString, attributeId.toString, seed) ~> check {
        assert(status == StatusCodes.Forbidden)
      }
    }

    "fail if verified attribute is not present in Tenant" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId = UUID.randomUUID()
      val attributeId    = UUID.randomUUID()

      val existingVerifier     = tenantVerifier.copy(id = UUID.randomUUID(), verificationDate = timestamp.minusDays(2))
      val existingVerification =
        dependencyVerifiedTenantAttribute(
          UUID.randomUUID(),
          assignmentTimestamp = timestamp.minusDays(1),
          verifiedBy = Seq(existingVerifier)
        )
      val tenant               = dependencyTenant.copy(
        id = targetTenantId,
        attributes = Seq(dependencyCertifiedTenantAttribute, dependencyDeclaredTenantAttribute, existingVerification)
      )

      val seed =
        UpdateVerifiedTenantAttributeSeed(Some(VerificationRenewal.AUTOMATIC_RENEWAL), Some(timestamp.plusDays(10)))

      mockDateTimeGet()

      mockGetTenantById(targetTenantId, tenant)

      Post() ~> tenantService.updateVerifiedAttribute(targetTenantId.toString, attributeId.toString, seed) ~> check {
        assert(status == StatusCodes.NotFound)
      }
    }
  }

  "Verified attribute revocation" should {
    val expectedAgreementStates: Seq[AgreementState] =
      Seq(AgreementState.PENDING, AgreementState.ACTIVE, AgreementState.SUSPENDED)

    "update the attribute of the target Tenant when present and add revoker" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId        = UUID.randomUUID()
      val attributeId           = UUID.randomUUID()
      val verifier              = tenantVerifier.copy(id = organizationId)
      val existingVerification  =
        dependencyVerifiedTenantAttribute(
          attributeId,
          verifiedBy = Seq(verifier),
          assignmentTimestamp = timestamp.minusDays(1)
        )
      val tenant                = dependencyTenant.copy(
        id = targetTenantId,
        attributes = Seq(dependencyCertifiedTenantAttribute, dependencyDeclaredTenantAttribute, existingVerification)
      )
      val (agreement, eService) = matchingAgreementAndEService(attributeId)

      val managementSeed = TenantAttribute(
        declared = None,
        certified = None,
        verified = Some(
          VerifiedTenantAttribute(
            id = attributeId,
            assignmentTimestamp = existingVerification.verified.get.assignmentTimestamp,
            verifiedBy = existingVerification.verified.get.verifiedBy.filterNot(_.id == verifier.id),
            revokedBy = existingVerification.verified.get.revokedBy :+ TenantRevoker(
              id = verifier.id,
              verificationDate = verifier.verificationDate,
              renewal = verifier.renewal,
              expirationDate = verifier.expirationDate,
              extensionDate = verifier.extensionDate,
              revocationDate = timestamp
            )
          )
        )
      )

      mockDateTimeGet()
      mockGetAgreements(organizationId, targetTenantId, expectedAgreementStates, Seq(agreement))
      mockGetEServiceById(eService.id, eService)
      mockGetTenantById(targetTenantId, tenant)
      mockUpdateTenantAttribute(targetTenantId, attributeId, managementSeed)
      mockComputeAgreementState(targetTenantId, attributeId)

      Post() ~> tenantService.revokeVerifiedAttribute(targetTenantId.toString, attributeId.toString) ~> check {
        assert(status == StatusCodes.OK)
      }
    }

    "fail with 403 if Tenant is revoking own attribute" in {
      implicit val context: Seq[(String, String)] = adminContext

      mockDateTimeGet()

      Post() ~> tenantService.revokeVerifiedAttribute(organizationId.toString, UUID.randomUUID().toString) ~> check {
        assert(status == StatusCodes.Forbidden)
      }
    }

    "fail with 403 if requester is not a Producer of an agreement containing the attribute" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId     = UUID.randomUUID()
      val attributeId        = UUID.randomUUID()
      val anotherAttributeId = UUID.randomUUID()

      val (agreement, eService) = matchingAgreementAndEService(anotherAttributeId)

      mockDateTimeGet()
      mockGetAgreements(organizationId, targetTenantId, expectedAgreementStates, Seq(agreement))
      mockGetEServiceById(eService.id, eService)

      Post() ~> tenantService.revokeVerifiedAttribute(targetTenantId.toString, attributeId.toString) ~> check {
        assert(status == StatusCodes.Forbidden)
      }
    }

    "fail with 403 if requester has not previously verified the attribute to the target Tenant" in {
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

      mockDateTimeGet()
      mockGetAgreements(organizationId, targetTenantId, expectedAgreementStates, Seq(agreement))
      mockGetEServiceById(eService.id, eService)
      mockGetTenantById(targetTenantId, tenant)

      Post() ~> tenantService.revokeVerifiedAttribute(targetTenantId.toString, attributeId.toString) ~> check {
        assert(status == StatusCodes.Forbidden)
      }
    }

  }

}
