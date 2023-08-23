package it.pagopa.interop.tenantprocess.authz

import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.utils.ORGANIZATION_ID_CLAIM
import it.pagopa.interop.commons.utils.service.{OffsetDateTimeSupplier, UUIDSupplier}
import it.pagopa.interop.tenantprocess.api._
import it.pagopa.interop.tenantprocess.api.impl._
import it.pagopa.interop.tenantprocess.model.{
  DeclaredTenantAttributeSeed,
  VerifiedTenantAttributeSeed,
  UpdateVerifiedTenantAttributeSeed
}
import it.pagopa.interop.tenantprocess.utils.AuthorizedRoutes.endpoints
import it.pagopa.interop.tenantprocess.utils.FakeDependencies._
import it.pagopa.interop.tenantprocess.utils.{ClusteredMUnitRouteTest, FakeDependencies, SpecData}

import java.time.OffsetDateTime
import java.util.UUID

class TenantApiServiceAuthzSpec extends ClusteredMUnitRouteTest with SpecData {
  val fakeAttributeRegistryManagement: FakeAttributeRegistryManagement = FakeAttributeRegistryManagement()
  val fakeAgreementManagement: FakeAgreementManagement                 = FakeAgreementManagement()
  val fakeCatalogManagement: FakeCatalogManagement                     = FakeCatalogManagement()
  val fakeTenantManagement: FakeTenantManagement                       = FakeTenantManagement()
  val fakeAgreementProcess: FakeAgreementProcess                       = FakeAgreementProcess()
  implicit val dummyReadModel: ReadModelService                        = new FakeReadModelService
  val dummyDateTimeSupplier: OffsetDateTimeSupplier                    = () => OffsetDateTime.now()
  val dummyUuidSupplier: UUIDSupplier                                  = () => UUID.randomUUID()

  val tenantService: TenantApiService = TenantApiServiceImpl(
    fakeAttributeRegistryManagement,
    fakeTenantManagement,
    fakeAgreementProcess,
    fakeAgreementManagement,
    fakeCatalogManagement,
    dummyUuidSupplier,
    dummyDateTimeSupplier
  )

  test("Tenant api should accept authorized roles for getProducers") {
    validateAuthorization(
      endpoints("getProducers"),
      { implicit c: Seq[(String, String)] => tenantService.getProducers(None, 0, 0) }
    )
  }

  test("Tenant api should accept authorized roles for getConsumers") {
    validateAuthorization(
      endpoints("getConsumers"),
      { implicit c: Seq[(String, String)] => tenantService.getConsumers(None, 0, 0) }
    )
  }

  test("Tenant api should accept authorized roles for getTenant") {
    validateAuthorization(
      endpoints("getTenant"),
      { implicit c: Seq[(String, String)] => tenantService.getTenant(UUID.randomUUID.toString) }
    )
  }

  test("Tenant api should accept authorized roles for getTenantBySelfcareId") {
    validateAuthorization(
      endpoints("getTenantBySelfcareId"),
      { implicit c: Seq[(String, String)] => tenantService.getTenantBySelfcareId(UUID.randomUUID.toString) }
    )
  }

  test("Tenant api should accept authorized roles for getTenantByExternalId") {
    validateAuthorization(
      endpoints("getTenantByExternalId"),
      { implicit c: Seq[(String, String)] =>
        tenantService.getTenantByExternalId("origin", "value")
      }
    )
  }

  test("Tenant api should accept authorized roles for internalUpsertTenant") {
    validateAuthorization(
      endpoints("internalUpsertTenant"),
      { implicit c: Seq[(String, String)] => tenantService.internalUpsertTenant(internalTenantSeed) }
    )
  }

  test("Tenant api should accept authorized roles for internalRevokeCertifiedAttribute") {
    validateAuthorization(
      endpoints("internalRevokeCertifiedAttribute"),
      { implicit c: Seq[(String, String)] => tenantService.internalRevokeCertifiedAttribute("", "", "", "") }
    )
  }

  test("Tenant api should accept authorized roles for internalAssignCertifiedAttribute") {
    validateAuthorization(
      endpoints("internalAssignCertifiedAttribute"),
      { implicit c: Seq[(String, String)] => tenantService.internalAssignCertifiedAttribute("", "", "", "") }
    )
  }

  test("Tenant api should accept authorized roles for m2mUpsertTenant") {
    validateAuthorization(
      endpoints("m2mUpsertTenant"),
      { implicit c: Seq[(String, String)] => tenantService.m2mUpsertTenant(m2mTenantSeed) }
    )
  }

  test("Tenant api should accept authorized roles for selfcareUpsertTenant") {
    validateAuthorization(
      endpoints("selfcareUpsertTenant"),
      { c: Seq[(String, String)] =>
        implicit val contextWithOrgId: Seq[(String, String)] =
          c.filter(
            _._1 != ORGANIZATION_ID_CLAIM
          ) :+ ORGANIZATION_ID_CLAIM -> FakeDependencies.fakePersistentTenant.id.toString
        tenantService.selfcareUpsertTenant(selfcareTenantSeed)
      }
    )
  }

  test("Tenant api should accept authorized roles for m2mDeleteAttribute") {
    validateAuthorization(
      endpoints("m2mDeleteAttribute"),
      { implicit c: Seq[(String, String)] => tenantService.m2mRevokeAttribute("foo", "bar", "baz") }
    )
  }

  test("Tenant api should accept authorized roles for addDeclaredAttribute") {
    validateAuthorization(
      endpoints("addDeclaredAttribute"),
      { implicit c: Seq[(String, String)] =>
        tenantService.addDeclaredAttribute(DeclaredTenantAttributeSeed(UUID.randomUUID()))
      }
    )
  }

  test("Tenant api should accept authorized roles for revokeDeclaredAttribute") {
    validateAuthorization(
      endpoints("revokeDeclaredAttribute"),
      { implicit c: Seq[(String, String)] => tenantService.revokeDeclaredAttribute("attributeId") }
    )
  }

  test("Tenant api should accept authorized roles for verifyVerifiedAttribute") {
    validateAuthorization(
      endpoints("verifyVerifiedAttribute"),
      { implicit c: Seq[(String, String)] =>
        tenantService.verifyVerifiedAttribute(
          c.find(_._1 == ORGANIZATION_ID_CLAIM).get.toString,
          VerifiedTenantAttributeSeed(verifiedAttributeId, None)
        )
      }
    )
  }

  test("Tenant api should accept authorized roles for revokeVerifiedAttribute") {
    validateAuthorization(
      endpoints("revokeVerifiedAttribute"),
      { implicit c: Seq[(String, String)] =>
        tenantService.revokeVerifiedAttribute(
          c.find(_._1 == ORGANIZATION_ID_CLAIM).get.toString,
          verifiedAttributeId.toString
        )
      }
    )
  }

  test("Tenant api should accept authorized roles for updateVerifiedAttribute") {
    validateAuthorization(
      endpoints("updateVerifiedAttribute"),
      { implicit c: Seq[(String, String)] =>
        tenantService.updateVerifiedAttribute(
          UUID.randomUUID().toString,
          UUID.randomUUID().toString,
          UpdateVerifiedTenantAttributeSeed(None)
        )
      }
    )
  }

  test("Tenant api should accept authorized roles for updateVerifiedAttributeExtensionDate") {
    validateAuthorization(
      endpoints("updateVerifiedAttributeExtensionDate"),
      { implicit c: Seq[(String, String)] =>
        tenantService.updateVerifiedAttributeExtensionDate(
          UUID.randomUUID().toString,
          UUID.randomUUID().toString,
          UUID.randomUUID().toString
        )
      }
    )
  }

  test("Tenant api should accept authorized roles for updateTenant") {
    validateAuthorization(
      endpoints("updateTenant"),
      { implicit c: Seq[(String, String)] =>
        tenantService.updateTenant(c.find(_._1 == ORGANIZATION_ID_CLAIM).get.toString, fakeTenantDelta)
      }
    )
  }
}
