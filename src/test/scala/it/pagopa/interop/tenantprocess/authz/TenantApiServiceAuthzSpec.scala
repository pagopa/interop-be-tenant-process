package it.pagopa.interop.tenantprocess.authz

import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.utils.service.{OffsetDateTimeSupplier, UUIDSupplier}
import it.pagopa.interop.tenantprocess.api._
import it.pagopa.interop.tenantprocess.api.impl._
import it.pagopa.interop.tenantprocess.model.{
  DeclaredTenantAttributeSeed,
  VerificationRenewal,
  VerifiedTenantAttributeSeed
}
import it.pagopa.interop.tenantprocess.utils.AuthorizedRoutes.endpoints
import it.pagopa.interop.tenantprocess.utils.FakeDependencies._
import it.pagopa.interop.tenantprocess.utils.{ClusteredMUnitRouteTest, SpecData}

import java.time.OffsetDateTime
import java.util.UUID

class TenantApiServiceAuthzSpec extends ClusteredMUnitRouteTest with SpecData {
  val fakeAttributeRegistryManagement: FakeAttributeRegistryManagement = FakeAttributeRegistryManagement()
  val fakeTenantManagement: FakeTenantManagement                       = FakeTenantManagement()
  val fakeAgreementManagement: FakeAgreementManagement                 = FakeAgreementManagement()
  val fakeCatalogManagement: FakeCatalogManagement                     = FakeCatalogManagement()
  val fakeAgreementProcess: FakeAgreementProcess                       = FakeAgreementProcess()
  val dummyReadModel: ReadModelService                                 = new ReadModelService(
    ReadModelConfig(
      "mongodb://localhost/?socketTimeoutMS=1&serverSelectionTimeoutMS=1&connectTimeoutMS=1&&autoReconnect=false&keepAlive=false",
      "db"
    )
  )
  val dummyDateTimeSupplier: OffsetDateTimeSupplier                    = () => OffsetDateTime.now()
  val dummyUuidSupplier: UUIDSupplier                                  = () => UUID.randomUUID()

  val tenantService: TenantApiService = TenantApiServiceImpl(
    fakeAttributeRegistryManagement,
    fakeTenantManagement,
    fakeAgreementProcess,
    fakeAgreementManagement,
    fakeCatalogManagement,
    dummyReadModel,
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

  test("Tenant api should accept authorized roles for internalUpsertTenant") {
    validateAuthorization(
      endpoints("internalUpsertTenant"),
      { implicit c: Seq[(String, String)] => tenantService.internalUpsertTenant(internalTenantSeed) }
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
      { implicit c: Seq[(String, String)] => tenantService.selfcareUpsertTenant(selfcareTenantSeed) }
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
          UUID.randomUUID().toString,
          VerifiedTenantAttributeSeed(verifiedAttributeId, VerificationRenewal.AUTOMATIC_RENEWAL, None)
        )
      }
    )
  }

  test("Tenant api should accept authorized roles for revokeVerifiedAttribute") {
    validateAuthorization(
      endpoints("revokeVerifiedAttribute"),
      { implicit c: Seq[(String, String)] =>
        tenantService.revokeVerifiedAttribute(UUID.randomUUID().toString, verifiedAttributeId.toString)
      }
    )
  }

  test("Tenant api should accept authorized roles for updateTenant") {
    validateAuthorization(
      endpoints("updateTenant"),
      { implicit c: Seq[(String, String)] =>
        tenantService.updateTenant(UUID.randomUUID().toString, fakeTenantDelta)
      }
    )
  }
}
