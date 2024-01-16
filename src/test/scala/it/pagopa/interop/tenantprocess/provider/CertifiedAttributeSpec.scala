package it.pagopa.interop.tenantprocess.provider

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.agreementprocess.client.model.CompactTenant
import it.pagopa.interop.tenantmanagement.client.{model => Dependency}
import it.pagopa.interop.tenantmanagement.model.tenant.{PersistentTenantFeature}
import it.pagopa.interop.attributeregistrymanagement.model.persistence.attribute.Declared
import it.pagopa.interop.tenantprocess.model.CertifiedTenantAttributeSeed
import it.pagopa.interop.tenantprocess.api.impl.TenantApiMarshallerImpl._
import it.pagopa.interop.tenantprocess.utils.SpecHelper
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class CertifiedAttributeSpec extends AnyWordSpecLike with SpecHelper with ScalatestRouteTest {

  "Certified attribute addition" should {
    "succeed" in {
      implicit val context: Seq[(String, String)] = adminContext

      val tenantId       = UUID.randomUUID()
      val attributeId    = UUID.randomUUID()
      val seed           = CertifiedTenantAttributeSeed(attributeId)
      val managementSeed = Dependency.TenantAttribute(
        declared = None,
        certified = Some(
          Dependency.CertifiedTenantAttribute(seed.id, assignmentTimestamp = timestamp, revocationTimestamp = None)
        ),
        verified = None
      )

      val requester = persistentTenant.copy(
        id = organizationId,
        features = List(PersistentTenantFeature.PersistentCertifier("certifier"))
      )

      val tenant = persistentTenant.copy(
        id = tenantId,
        attributes = List(persistentCertifiedAttribute, persistentDeclaredAttribute, persistentVerifiedAttribute)
      )

      mockDateTimeGet()
      mockGetTenantById(organizationId, requester)
      mockGetTenantById(tenantId, tenant)
      mockGetAttributeById(seed.id, persistentAttribute.copy(id = seed.id))
      mockAddTenantAttribute(tenantId, managementSeed)
      mockUpdateTenant(
        tenantId,
        Dependency.TenantDelta(
          selfcareId = None,
          features = Nil,
          kind = Dependency.TenantKind.PA,
          onboardedAt = None,
          subUnitType = None
        )
      )
      mockComputeAgreementState(attributeId, CompactTenant(tenantId, Nil))

      Post() ~> tenantService.addCertifiedAttribute(tenantId.toString, seed) ~> check {
        assert(status == StatusCodes.OK)
      }
    }
  }
  "fail if requester is not a certifier" in {
    implicit val context: Seq[(String, String)] = adminContext

    val tenantId    = UUID.randomUUID()
    val attributeId = UUID.randomUUID()
    val seed        = CertifiedTenantAttributeSeed(attributeId)

    val requester = persistentTenant.copy(id = organizationId)

    mockDateTimeGet()
    mockGetTenantById(organizationId, requester)

    Post() ~> tenantService.addCertifiedAttribute(tenantId.toString, seed) ~> check {
      assert(status == StatusCodes.Forbidden)
    }
  }

  "fail if attribute does not exists" in {
    implicit val context: Seq[(String, String)] = adminContext

    val tenantId    = UUID.randomUUID()
    val attributeId = UUID.randomUUID()
    val seed        = CertifiedTenantAttributeSeed(attributeId)

    val requester = persistentTenant.copy(
      id = organizationId,
      features = List(PersistentTenantFeature.PersistentCertifier("certifier"))
    )

    mockDateTimeGet()
    mockGetTenantById(organizationId, requester)
    mockGetAttributeByIdNotFound(seed.id)

    Post() ~> tenantService.addCertifiedAttribute(tenantId.toString, seed) ~> check {
      assert(status == StatusCodes.InternalServerError)
    }
  }

  "fail if attribute exists but is not certified" in {
    implicit val context: Seq[(String, String)] = adminContext

    val tenantId    = UUID.randomUUID()
    val attributeId = UUID.randomUUID()
    val seed        = CertifiedTenantAttributeSeed(attributeId)

    val requester = persistentTenant.copy(
      id = organizationId,
      features = List(PersistentTenantFeature.PersistentCertifier("certifier"))
    )

    mockDateTimeGet()
    mockGetTenantById(organizationId, requester)
    mockGetAttributeById(seed.id, persistentAttribute.copy(id = seed.id, kind = Declared))

    Post() ~> tenantService.addCertifiedAttribute(tenantId.toString, seed) ~> check {
      assert(status == StatusCodes.InternalServerError)
    }
  }

  "fail if certified tenant attribute already exists" in {
    implicit val context: Seq[(String, String)] = adminContext

    val tenantId    = UUID.randomUUID()
    val attributeId = UUID.randomUUID()
    val seed        = CertifiedTenantAttributeSeed(attributeId)

    val requester = persistentTenant.copy(
      id = organizationId,
      features = List(PersistentTenantFeature.PersistentCertifier("certifier"))
    )

    val tenant = persistentTenant.copy(
      id = tenantId,
      attributes =
        List(persistentCertifiedAttribute.copy(id = seed.id), persistentDeclaredAttribute, persistentVerifiedAttribute)
    )

    mockDateTimeGet()
    mockGetTenantById(organizationId, requester)
    mockGetTenantById(tenantId, tenant)
    mockGetAttributeById(seed.id, persistentAttribute.copy(id = seed.id))

    Post() ~> tenantService.addCertifiedAttribute(tenantId.toString, seed) ~> check {
      assert(status == StatusCodes.Conflict)
    }
  }

  "Certified attribute revoke" should {
    "succeed" in {
      implicit val context: Seq[(String, String)] = adminContext

      val tenantId    = UUID.randomUUID()
      val attributeId = UUID.randomUUID()

      val requester = persistentTenant.copy(
        id = organizationId,
        features = List(PersistentTenantFeature.PersistentCertifier("certifier"))
      )

      val tenant = persistentTenant.copy(
        id = tenantId,
        attributes = List(
          persistentCertifiedAttribute.copy(id = attributeId),
          persistentDeclaredAttribute,
          persistentVerifiedAttribute
        )
      )

      val managementSeed = Dependency.TenantAttribute(
        declared = None,
        certified = Some(
          Dependency.CertifiedTenantAttribute(
            attributeId,
            assignmentTimestamp = timestamp,
            revocationTimestamp = Some(timestamp)
          )
        ),
        verified = None
      )

      mockDateTimeGet()
      mockGetTenantById(organizationId, requester)
      mockGetTenantById(tenantId, tenant)
      mockGetAttributeById(attributeId, persistentAttribute.copy(id = attributeId))
      mockUpdateTenantAttribute(tenantId, attributeId, managementSeed)
      mockUpdateTenant(
        tenantId,
        Dependency.TenantDelta(
          selfcareId = None,
          features = Nil,
          kind = Dependency.TenantKind.PA,
          onboardedAt = None,
          subUnitType = None
        )
      )

      mockComputeAgreementState(attributeId, CompactTenant(tenantId, Nil))

      Delete() ~> tenantService.revokeCertifiedAttributeById(tenantId.toString, attributeId.toString) ~> check {
        assert(status == StatusCodes.NoContent)
      }
    }
  }
  "revoke fail if requester is not a certifier" in {
    implicit val context: Seq[(String, String)] = adminContext

    val tenantId    = UUID.randomUUID()
    val attributeId = UUID.randomUUID()

    val requester = persistentTenant.copy(id = organizationId)

    mockDateTimeGet()
    mockGetTenantById(organizationId, requester)

    Delete() ~> tenantService.revokeCertifiedAttributeById(tenantId.toString, attributeId.toString) ~> check {
      assert(status == StatusCodes.Forbidden)
    }
  }

  "revoke fail if attribute does not exists" in {
    implicit val context: Seq[(String, String)] = adminContext

    val tenantId    = UUID.randomUUID()
    val attributeId = UUID.randomUUID()

    val requester = persistentTenant.copy(
      id = organizationId,
      features = List(PersistentTenantFeature.PersistentCertifier("certifier"))
    )

    mockDateTimeGet()
    mockGetTenantById(organizationId, requester)
    mockGetAttributeByIdNotFound(attributeId)

    Delete() ~> tenantService.revokeCertifiedAttributeById(tenantId.toString, attributeId.toString) ~> check {
      assert(status == StatusCodes.InternalServerError)
    }
  }

  "revoke fail if attribute exists but is not certified" in {
    implicit val context: Seq[(String, String)] = adminContext

    val tenantId    = UUID.randomUUID()
    val attributeId = UUID.randomUUID()

    val requester = persistentTenant.copy(
      id = organizationId,
      features = List(PersistentTenantFeature.PersistentCertifier("certifier"))
    )

    mockDateTimeGet()
    mockGetTenantById(organizationId, requester)
    mockGetAttributeById(attributeId, persistentAttribute.copy(id = attributeId, kind = Declared))

    Delete() ~> tenantService.revokeCertifiedAttributeById(tenantId.toString, attributeId.toString) ~> check {
      assert(status == StatusCodes.InternalServerError)
    }
  }
  "revoke fail if attribute does not exists on tenant" in {
    implicit val context: Seq[(String, String)] = adminContext

    val tenantId    = UUID.randomUUID()
    val attributeId = UUID.randomUUID()

    val requester = persistentTenant.copy(
      id = organizationId,
      features = List(PersistentTenantFeature.PersistentCertifier("certifier"))
    )

    val tenant = persistentTenant.copy(
      id = tenantId,
      attributes = List(persistentCertifiedAttribute, persistentDeclaredAttribute, persistentVerifiedAttribute)
    )

    mockDateTimeGet()
    mockGetTenantById(organizationId, requester)
    mockGetTenantById(tenantId, tenant)
    mockGetAttributeById(attributeId, persistentAttribute.copy(id = attributeId))

    Delete() ~> tenantService.revokeCertifiedAttributeById(tenantId.toString, attributeId.toString) ~> check {
      assert(status == StatusCodes.BadRequest)
    }
  }
}
