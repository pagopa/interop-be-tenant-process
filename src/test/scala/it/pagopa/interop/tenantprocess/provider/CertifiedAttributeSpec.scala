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

      val tenantUuid     = UUID.randomUUID()
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
        id = tenantUuid,
        attributes = List(persistentCertifiedAttribute, persistentDeclaredAttribute, persistentVerifiedAttribute)
      )

      mockDateTimeGet()
      mockGetTenantById(organizationId, requester)
      mockGetTenantById(tenantUuid, tenant)
      mockGetAttributeById(seed.id, persistentAttribute.copy(id = seed.id))
      mockAddTenantAttribute(tenantUuid, managementSeed)
      mockComputeAgreementState(attributeId, CompactTenant(tenantUuid, Nil))

      Post() ~> tenantService.addCertifiedAttribute(tenantUuid.toString, seed) ~> check {
        assert(status == StatusCodes.OK)
      }
    }
  }
  "fail if requester is not a certifier" in {
    implicit val context: Seq[(String, String)] = adminContext

    val tenantUuid  = UUID.randomUUID()
    val attributeId = UUID.randomUUID()
    val seed        = CertifiedTenantAttributeSeed(attributeId)

    val requester = persistentTenant.copy(id = organizationId)

    mockDateTimeGet()
    mockGetTenantById(organizationId, requester)

    Post() ~> tenantService.addCertifiedAttribute(tenantUuid.toString, seed) ~> check {
      assert(status == StatusCodes.Forbidden)
    }
  }

  "fail if attribute does not exists" in {
    implicit val context: Seq[(String, String)] = adminContext

    val tenantUuid  = UUID.randomUUID()
    val attributeId = UUID.randomUUID()
    val seed        = CertifiedTenantAttributeSeed(attributeId)

    val requester = persistentTenant.copy(
      id = organizationId,
      features = List(PersistentTenantFeature.PersistentCertifier("certifier"))
    )

    mockDateTimeGet()
    mockGetTenantById(organizationId, requester)
    mockGetAttributeByIdNotFound(seed.id)

    Post() ~> tenantService.addCertifiedAttribute(tenantUuid.toString, seed) ~> check {
      assert(status == StatusCodes.InternalServerError)
    }
  }

  "fail if attribute exists but is not certified" in {
    implicit val context: Seq[(String, String)] = adminContext

    val tenantUuid  = UUID.randomUUID()
    val attributeId = UUID.randomUUID()
    val seed        = CertifiedTenantAttributeSeed(attributeId)

    val requester = persistentTenant.copy(
      id = organizationId,
      features = List(PersistentTenantFeature.PersistentCertifier("certifier"))
    )

    mockDateTimeGet()
    mockGetTenantById(organizationId, requester)
    mockGetAttributeById(seed.id, persistentAttribute.copy(id = seed.id, kind = Declared))

    Post() ~> tenantService.addCertifiedAttribute(tenantUuid.toString, seed) ~> check {
      assert(status == StatusCodes.InternalServerError)
    }
  }

  "fail if certified tenant attribute already exists" in {
    implicit val context: Seq[(String, String)] = adminContext

    val tenantUuid  = UUID.randomUUID()
    val attributeId = UUID.randomUUID()
    val seed        = CertifiedTenantAttributeSeed(attributeId)

    val requester = persistentTenant.copy(
      id = organizationId,
      features = List(PersistentTenantFeature.PersistentCertifier("certifier"))
    )

    val tenant = persistentTenant.copy(
      id = tenantUuid,
      attributes =
        List(persistentCertifiedAttribute.copy(id = seed.id), persistentDeclaredAttribute, persistentVerifiedAttribute)
    )

    mockDateTimeGet()
    mockGetTenantById(organizationId, requester)
    mockGetTenantById(tenantUuid, tenant)
    mockGetAttributeById(seed.id, persistentAttribute.copy(id = seed.id))

    Post() ~> tenantService.addCertifiedAttribute(tenantUuid.toString, seed) ~> check {
      assert(status == StatusCodes.Conflict)
    }
  }
}
