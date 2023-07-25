package it.pagopa.interop.tenantprocess.provider

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.agreementprocess.client.model.CompactTenant
import it.pagopa.interop.tenantmanagement.client.{model => Dependency}
import it.pagopa.interop.tenantprocess.api.adapters.ReadModelTenantAdapters._
import it.pagopa.interop.tenantprocess.api.adapters.TenantManagementAdapters._
import it.pagopa.interop.tenantprocess.api.impl.TenantApiMarshallerImpl._
import it.pagopa.interop.tenantprocess.model.{UpdateVerifiedTenantAttributeSeed, VerifiedTenantAttributeSeed}
import it.pagopa.interop.tenantprocess.utils.SpecHelper
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.Duration
import java.util.UUID

class VerifiedAttributeSpec extends AnyWordSpecLike with SpecHelper with ScalatestRouteTest {

  "Verified attribute verification" should {
    "add the attribute to the target Tenant when not present" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId        = UUID.randomUUID()
      val attributeId           = UUID.randomUUID()
      val tenant                = persistentTenant.copy(
        id = targetTenantId,
        attributes = List(persistentCertifiedAttribute, persistentDeclaredAttribute, persistentVerifiedAttribute)
      )
      val (agreement, eService) = matchingAgreementAndEService(attributeId)

      val seed           = VerifiedTenantAttributeSeed(attributeId, Some(timestamp))
      val attribute      = Dependency.VerifiedTenantAttribute(
        id = seed.id,
        assignmentTimestamp = timestamp,
        verifiedBy = Seq(
          Dependency.TenantVerifier(
            id = organizationId,
            verificationDate = timestamp,
            expirationDate = seed.expirationDate,
            extensionDate = seed.expirationDate
          )
        ),
        revokedBy = Nil
      )
      val managementSeed = Dependency.TenantAttribute(declared = None, certified = None, verified = Some(attribute))

      val managementTenant = tenant.toManagement
      val updatedTenant    = managementTenant.copy(attributes =
        managementTenant.attributes :+ Dependency.TenantAttribute(verified = Some(attribute))
      )

      val compactTenant = CompactTenant(targetTenantId, updatedTenant.attributes.map(_.toAgreementApi))

      mockDateTimeGet()
      mockGetAgreements(Seq(agreement))
      mockGetEServiceById(eService.id, eService)
      mockGetTenantById(targetTenantId, tenant)
      mockAddTenantAttribute(targetTenantId, managementSeed, updatedTenant)
      mockComputeAgreementState(attributeId, compactTenant)

      Post() ~> tenantService.verifyVerifiedAttribute(targetTenantId.toString, seed) ~> check {
        assert(status == StatusCodes.OK)
      }
    }

    "update the attribute of the target Tenant when present and add verifier" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId                 = UUID.randomUUID()
      val attributeId                    = UUID.randomUUID()
      val existingVerification           =
        persistentVerifiedAttribute.copy(id = attributeId, assignmentTimestamp = timestamp.minusDays(1))
      val tenant                         = persistentTenant.copy(
        id = targetTenantId,
        attributes = List(persistentCertifiedAttribute, persistentDeclaredAttribute, existingVerification)
      )
      val dependencyExistingVerification = existingVerification.toManagement
      val (agreement, eService)          = matchingAgreementAndEService(attributeId)

      val seed      = VerifiedTenantAttributeSeed(attributeId, Some(timestamp))
      val attribute = Dependency.VerifiedTenantAttribute(
        id = seed.id,
        assignmentTimestamp = dependencyExistingVerification.assignmentTimestamp,
        verifiedBy = dependencyExistingVerification.verifiedBy :+
          Dependency.TenantVerifier(
            id = organizationId,
            verificationDate = timestamp,
            expirationDate = seed.expirationDate,
            extensionDate = None
          ),
        revokedBy = dependencyExistingVerification.revokedBy
      )

      val managementSeed = Dependency.TenantAttribute(declared = None, certified = None, verified = Some(attribute))

      val managementTenant = tenant.toManagement
      val updatedTenant    = managementTenant.copy(attributes =
        managementTenant.attributes :+ Dependency.TenantAttribute(verified = Some(attribute))
      )

      val compactTenant = CompactTenant(targetTenantId, updatedTenant.attributes.map(_.toAgreementApi))

      mockDateTimeGet()
      mockGetAgreements(Seq(agreement))
      mockGetEServiceById(eService.id, eService)
      mockGetTenantById(targetTenantId, tenant)
      mockUpdateTenantAttribute(targetTenantId, seed.id, managementSeed, updatedTenant)
      mockComputeAgreementState(attributeId, compactTenant)

      Post() ~> tenantService.verifyVerifiedAttribute(targetTenantId.toString, seed) ~> check {
        assert(status == StatusCodes.OK)
      }
    }

    "update the attribute of the target Tenant adding verifier and keeping revoker when previously revoked" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId       = UUID.randomUUID()
      val attributeId          = UUID.randomUUID()
      val existingRevocation   =
        persistentTenantRevoker.copy(id = organizationId, verificationDate = timestamp.minusDays(2))
      val existingVerification =
        persistentVerifiedAttribute.copy(
          id = attributeId,
          revokedBy = List(existingRevocation),
          assignmentTimestamp = timestamp.minusDays(1)
        )

      val tenant = persistentTenant.copy(
        id = targetTenantId,
        attributes = List(persistentCertifiedAttribute, persistentDeclaredAttribute, existingVerification)
      )

      val dependencyExistingVerification = existingVerification.toManagement

      val (agreement, eService) = matchingAgreementAndEService(attributeId)

      val seed      = VerifiedTenantAttributeSeed(attributeId, Some(timestamp))
      val attribute = Dependency.VerifiedTenantAttribute(
        id = seed.id,
        assignmentTimestamp = dependencyExistingVerification.assignmentTimestamp,
        verifiedBy = dependencyExistingVerification.verifiedBy :+
          Dependency.TenantVerifier(
            id = organizationId,
            verificationDate = timestamp,
            expirationDate = seed.expirationDate,
            extensionDate = None
          ),
        revokedBy = dependencyExistingVerification.revokedBy
      )

      val managementSeed = Dependency.TenantAttribute(declared = None, certified = None, verified = Some(attribute))

      val managementTenant = tenant.toManagement
      val updatedTenant    = managementTenant.copy(attributes =
        managementTenant.attributes.filterNot(_.verified.exists(_.id == attribute.id)) :+ Dependency
          .TenantAttribute(verified = Some(attribute))
      )

      val compactTenant = CompactTenant(targetTenantId, updatedTenant.attributes.map(_.toAgreementApi))

      mockDateTimeGet()
      mockGetAgreements(Seq(agreement))
      mockGetEServiceById(eService.id, eService)
      mockGetTenantById(targetTenantId, tenant)
      mockUpdateTenantAttribute(targetTenantId, seed.id, managementSeed, updatedTenant)
      mockComputeAgreementState(attributeId, compactTenant)

      Post() ~> tenantService.verifyVerifiedAttribute(targetTenantId.toString, seed) ~> check {
        assert(status == StatusCodes.OK)
      }
    }

    "fail with 403 if Tenant is verifying own attribute" in {
      implicit val context: Seq[(String, String)] = adminContext

      val seed = VerifiedTenantAttributeSeed(UUID.randomUUID(), None)

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

      val seed = VerifiedTenantAttributeSeed(attributeId, Some(timestamp))

      mockDateTimeGet()
      mockGetAgreements(Seq(agreement))
      mockGetEServiceById(eService.id, eService)

      Post() ~> tenantService.verifyVerifiedAttribute(targetTenantId.toString, seed) ~> check {
        assert(status == StatusCodes.Forbidden)
      }
    }

    "succeed if requester has already verified the attribute for target Tenant" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId = UUID.randomUUID()
      val attributeId    = UUID.randomUUID()

      val existingVerifier     =
        persistentTenantVerifier.copy(id = organizationId, verificationDate = timestamp.minusDays(2))
      val existingVerification =
        persistentVerifiedAttribute.copy(
          attributeId,
          assignmentTimestamp = timestamp.minusDays(1),
          verifiedBy = List(existingVerifier)
        )

      val tenant                         = persistentTenant.copy(
        id = targetTenantId,
        attributes = List(persistentCertifiedAttribute, persistentDeclaredAttribute, existingVerification)
      )
      val dependencyExistingVerification = existingVerification.toManagement

      val (agreement, eService) = matchingAgreementAndEService(attributeId)

      val seed = VerifiedTenantAttributeSeed(attributeId, Some(timestamp))

      val attribute = Dependency.VerifiedTenantAttribute(
        id = seed.id,
        assignmentTimestamp = dependencyExistingVerification.assignmentTimestamp,
        verifiedBy = dependencyExistingVerification.verifiedBy :+
          Dependency.TenantVerifier(
            id = organizationId,
            verificationDate = timestamp,
            expirationDate = seed.expirationDate,
            extensionDate = None
          ),
        revokedBy = dependencyExistingVerification.revokedBy
      )

      val managementSeed = Dependency.TenantAttribute(declared = None, certified = None, verified = Some(attribute))

      val managementTenant = tenant.toManagement
      val updatedTenant    = managementTenant.copy(attributes =
        managementTenant.attributes.filterNot(_.verified.exists(_.id == attribute.id)) :+ Dependency
          .TenantAttribute(verified = Some(attribute))
      )

      val compactTenant = CompactTenant(targetTenantId, updatedTenant.attributes.map(_.toAgreementApi))

      mockDateTimeGet()
      mockGetAgreements(Seq(agreement))
      mockGetEServiceById(eService.id, eService)
      mockGetTenantById(targetTenantId, tenant)
      mockUpdateTenantAttribute(targetTenantId, attributeId, managementSeed, updatedTenant)
      mockComputeAgreementState(attributeId, compactTenant)

      Post() ~> tenantService.verifyVerifiedAttribute(targetTenantId.toString, seed) ~> check {
        assert(status == StatusCodes.OK)
      }
    }
  }

  "Verified attribute update strategy" should {
    "succeed" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId = UUID.randomUUID()
      val attributeId    = UUID.randomUUID()

      val existingVerifier     =
        persistentTenantVerifier.copy(id = organizationId, verificationDate = timestamp.minusDays(2))
      val existingVerification =
        persistentVerifiedAttribute.copy(
          attributeId,
          assignmentTimestamp = timestamp.minusDays(1),
          verifiedBy = List(existingVerifier)
        )

      val tenant                         = persistentTenant.copy(
        id = targetTenantId,
        attributes = List(persistentCertifiedAttribute, persistentDeclaredAttribute, existingVerification)
      )
      val dependencyExistingVerification = existingVerification.toManagement

      val seed =
        UpdateVerifiedTenantAttributeSeed(Some(timestamp.plusDays(10)))

      val managementSeed = Dependency.TenantAttribute(
        declared = None,
        certified = None,
        verified = Some(
          Dependency.VerifiedTenantAttribute(
            id = attributeId,
            assignmentTimestamp = dependencyExistingVerification.assignmentTimestamp,
            verifiedBy = dependencyExistingVerification.verifiedBy.filterNot(_.id == organizationId) :+
              Dependency.TenantVerifier(
                id = organizationId,
                verificationDate = timestamp,
                expirationDate = seed.expirationDate,
                extensionDate = None
              ),
            revokedBy = dependencyExistingVerification.revokedBy
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
        UpdateVerifiedTenantAttributeSeed(Some(timestamp.minusDays(2)))

      mockDateTimeGet()

      Post() ~> tenantService.updateVerifiedAttribute(targetTenantId.toString, attributeId.toString, seed) ~> check {
        assert(status == StatusCodes.BadRequest)
      }
    }

    "fail if Requester is not a previous verifier of verified attribute" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId = UUID.randomUUID()
      val attributeId    = UUID.randomUUID()

      val existingVerifier     =
        persistentTenantVerifier.copy(id = UUID.randomUUID(), verificationDate = timestamp.minusDays(2))
      val existingVerification =
        persistentVerifiedAttribute.copy(
          attributeId,
          assignmentTimestamp = timestamp.minusDays(1),
          verifiedBy = List(existingVerifier)
        )

      val tenant = persistentTenant.copy(
        id = targetTenantId,
        attributes = List(persistentCertifiedAttribute, persistentDeclaredAttribute, existingVerification)
      )

      val seed =
        UpdateVerifiedTenantAttributeSeed(Some(timestamp.plusDays(10)))

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

      val existingVerifier     =
        persistentTenantVerifier.copy(id = UUID.randomUUID(), verificationDate = timestamp.minusDays(2))
      val existingVerification =
        persistentVerifiedAttribute.copy(
          UUID.randomUUID(),
          assignmentTimestamp = timestamp.minusDays(1),
          verifiedBy = List(existingVerifier)
        )

      val tenant = persistentTenant.copy(
        id = targetTenantId,
        attributes = List(persistentCertifiedAttribute, persistentDeclaredAttribute, existingVerification)
      )

      val seed =
        UpdateVerifiedTenantAttributeSeed(Some(timestamp.plusDays(10)))

      mockDateTimeGet()

      mockGetTenantById(targetTenantId, tenant)

      Post() ~> tenantService.updateVerifiedAttribute(targetTenantId.toString, attributeId.toString, seed) ~> check {
        assert(status == StatusCodes.NotFound)
      }
    }
  }

  "Verified attribute update extensionDate" should {
    "succeed" in {
      implicit val context: Seq[(String, String)] = internalContext

      val targetTenantId = UUID.randomUUID()
      val attributeId    = UUID.randomUUID()

      val existingVerifier     =
        persistentTenantVerifier.copy(id = organizationId, expirationDate = Some(timestamp.plusDays(30)))
      val existingVerification =
        persistentVerifiedAttribute.copy(
          attributeId,
          assignmentTimestamp = timestamp.minusDays(1),
          verifiedBy = List(existingVerifier)
        )

      val tenant                         = persistentTenant.copy(
        id = targetTenantId,
        attributes = List(persistentCertifiedAttribute, persistentDeclaredAttribute, existingVerification)
      )
      val dependencyExistingVerification = existingVerification.toManagement

      val managementSeed = Dependency.TenantAttribute(
        declared = None,
        certified = None,
        verified = Some(
          Dependency.VerifiedTenantAttribute(
            id = attributeId,
            assignmentTimestamp = dependencyExistingVerification.assignmentTimestamp,
            verifiedBy = dependencyExistingVerification.verifiedBy.filterNot(_.id == organizationId) :+
              Dependency.TenantVerifier(
                id = organizationId,
                verificationDate = timestamp,
                expirationDate = existingVerifier.expirationDate,
                extensionDate = Some(
                  existingVerifier.extensionDate.get
                    .plus(Duration.between(existingVerifier.verificationDate, existingVerifier.expirationDate.get))
                )
              ),
            revokedBy = dependencyExistingVerification.revokedBy
          )
        )
      )
      mockGetTenantById(targetTenantId, tenant)
      mockUpdateTenantAttribute(targetTenantId, attributeId, managementSeed)

      Post() ~> tenantService.updateVerifiedAttributeExtensionDate(
        targetTenantId.toString,
        attributeId.toString,
        organizationId.toString
      ) ~> check {
        assert(status == StatusCodes.OK)
      }
    }
  }
  "fail if Requester is not a previous verifier of verified attribute" in {
    implicit val context: Seq[(String, String)] = internalContext

    val targetTenantId = UUID.randomUUID()
    val attributeId    = UUID.randomUUID()

    val existingVerifier     =
      persistentTenantVerifier.copy(id = UUID.randomUUID(), verificationDate = timestamp.minusDays(2))
    val existingVerification =
      persistentVerifiedAttribute.copy(
        attributeId,
        assignmentTimestamp = timestamp.minusDays(1),
        verifiedBy = List(existingVerifier)
      )

    val tenant = persistentTenant.copy(
      id = targetTenantId,
      attributes = List(persistentCertifiedAttribute, persistentDeclaredAttribute, existingVerification)
    )

    mockGetTenantById(targetTenantId, tenant)

    Post() ~> tenantService.updateVerifiedAttributeExtensionDate(
      targetTenantId.toString,
      attributeId.toString,
      organizationId.toString
    ) ~> check {
      assert(status == StatusCodes.Forbidden)
    }
  }
  "fail if verified attribute is not present in Tenant" in {
    implicit val context: Seq[(String, String)] = internalContext

    val targetTenantId = UUID.randomUUID()
    val attributeId    = UUID.randomUUID()

    val existingVerifier     =
      persistentTenantVerifier.copy(id = UUID.randomUUID(), verificationDate = timestamp.minusDays(2))
    val existingVerification =
      persistentVerifiedAttribute.copy(
        UUID.randomUUID(),
        assignmentTimestamp = timestamp.minusDays(1),
        verifiedBy = List(existingVerifier)
      )
    val tenant               = persistentTenant.copy(
      id = targetTenantId,
      attributes = List(persistentCertifiedAttribute, persistentDeclaredAttribute, existingVerification)
    )

    mockGetTenantById(targetTenantId, tenant)

    Post() ~> tenantService.updateVerifiedAttributeExtensionDate(
      targetTenantId.toString,
      attributeId.toString,
      organizationId.toString
    ) ~> check {
      assert(status == StatusCodes.NotFound)
    }
  }

  "Verified attribute revocation" should {

    "update the attribute of the target Tenant when present and add revoker" in {
      implicit val context: Seq[(String, String)] = adminContext

      val targetTenantId                 = UUID.randomUUID()
      val attributeId                    = UUID.randomUUID()
      val verifier                       = persistentTenantVerifier.copy(id = organizationId)
      val existingVerification           =
        persistentVerifiedAttribute.copy(
          attributeId,
          verifiedBy = List(verifier),
          assignmentTimestamp = timestamp.minusDays(1)
        )
      val tenant                         = persistentTenant.copy(
        id = targetTenantId,
        attributes = List(persistentCertifiedAttribute, persistentDeclaredAttribute, existingVerification)
      )
      val dependencyExistingVerification = existingVerification.toManagement

      val (agreement, eService) = matchingAgreementAndEService(attributeId)

      val attribute = Dependency.VerifiedTenantAttribute(
        id = attributeId,
        assignmentTimestamp = dependencyExistingVerification.assignmentTimestamp,
        verifiedBy = dependencyExistingVerification.verifiedBy.filterNot(_.id == verifier.id),
        revokedBy = dependencyExistingVerification.revokedBy :+ Dependency.TenantRevoker(
          id = verifier.id,
          verificationDate = verifier.verificationDate,
          expirationDate = verifier.expirationDate,
          extensionDate = verifier.extensionDate,
          revocationDate = timestamp
        )
      )

      val managementSeed = Dependency.TenantAttribute(declared = None, certified = None, verified = Some(attribute))

      val managementTenant = tenant.toManagement
      val updatedTenant    = managementTenant.copy(attributes =
        managementTenant.attributes.filterNot(_.verified.exists(_.id == attribute.id)) :+ Dependency
          .TenantAttribute(verified = Some(attribute))
      )

      val compactTenant = CompactTenant(targetTenantId, updatedTenant.attributes.map(_.toAgreementApi))

      mockDateTimeGet()
      mockGetAgreements(Seq(agreement))
      mockGetEServiceById(eService.id, eService)
      mockGetTenantById(targetTenantId, tenant)
      mockUpdateTenantAttribute(targetTenantId, attributeId, managementSeed, updatedTenant)
      mockComputeAgreementState(attributeId, compactTenant)

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
      mockGetAgreements(Seq(agreement))
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
        persistentVerifiedAttribute.copy(attributeId, assignmentTimestamp = timestamp.minusDays(1))
      val tenant                = persistentTenant.copy(
        id = targetTenantId,
        attributes = List(persistentCertifiedAttribute, persistentDeclaredAttribute, existingVerification)
      )
      val (agreement, eService) = matchingAgreementAndEService(attributeId)

      mockDateTimeGet()
      mockGetAgreements(Seq(agreement))
      mockGetEServiceById(eService.id, eService)
      mockGetTenantById(targetTenantId, tenant)

      Post() ~> tenantService.revokeVerifiedAttribute(targetTenantId.toString, attributeId.toString) ~> check {
        assert(status == StatusCodes.Forbidden)
      }
    }
  }
}
