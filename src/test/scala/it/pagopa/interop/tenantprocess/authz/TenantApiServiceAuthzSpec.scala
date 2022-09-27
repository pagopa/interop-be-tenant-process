package it.pagopa.interop.tenantprocess.authz

import it.pagopa.interop.commons.utils.service.{OffsetDateTimeSupplier, UUIDSupplier}
import it.pagopa.interop.tenantprocess.api._
import it.pagopa.interop.tenantprocess.api.impl._
import it.pagopa.interop.tenantprocess.model.DeclaredTenantAttributeSeed
import it.pagopa.interop.tenantprocess.utils.AuthorizedRoutes.endpoints
import it.pagopa.interop.tenantprocess.utils.FakeDependencies.{FakeAttributeRegistryManagement, FakeTenantManagement}
import it.pagopa.interop.tenantprocess.utils.{ClusteredMUnitRouteTest, SpecData}

import java.time.OffsetDateTime
import java.util.UUID

class TenantApiServiceAuthzSpec extends ClusteredMUnitRouteTest with SpecData {
  val fakeAttributeRegistryManagement: FakeAttributeRegistryManagement = FakeAttributeRegistryManagement()
  val fakeTenantManagement: FakeTenantManagement                       = FakeTenantManagement()
  val dummyDateTimeSupplier: OffsetDateTimeSupplier                    = new OffsetDateTimeSupplier {
    def get: OffsetDateTime = OffsetDateTime.now()
  }
  val dummyUuidSupplier: UUIDSupplier                                  = new UUIDSupplier {
    def get: UUID = UUID.randomUUID()
  }

  val tenantService: TenantApiService = TenantApiServiceImpl(
    fakeAttributeRegistryManagement,
    fakeTenantManagement,
    dummyUuidSupplier,
    dummyDateTimeSupplier
  )

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
}
