package it.pagopa.interop.tenantprocess.utils

import it.pagopa.interop.attributeregistrymanagement.client.model.Attribute
import it.pagopa.interop.commons.utils._
import it.pagopa.interop.commons.utils.service.{OffsetDateTimeSupplier, UUIDSupplier}
import it.pagopa.interop.tenantmanagement.client.invoker.ApiError
import it.pagopa.interop.tenantmanagement.client.model.{ExternalId, Tenant, TenantAttribute, TenantDelta, TenantSeed}
import it.pagopa.interop.tenantprocess.api.TenantApiService
import it.pagopa.interop.tenantprocess.api.impl.TenantApiServiceImpl
import it.pagopa.interop.tenantprocess.service.{AttributeRegistryManagementService, TenantManagementService}
import org.scalamock.scalatest.MockFactory

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait SpecHelper extends MockFactory with SpecData {

  val bearerToken          = "token"
  val organizationId: UUID = UUID.randomUUID()

  val userContext: Seq[(String, String)]     =
    Seq("bearer" -> bearerToken, USER_ROLES -> "admin", UID -> UUID.randomUUID().toString)
  val m2mContext: Seq[(String, String)]      =
    Seq("bearer" -> bearerToken, USER_ROLES -> "m2m", ORGANIZATION_ID_CLAIM -> organizationId.toString)
  val internalContext: Seq[(String, String)] =
    Seq("bearer" -> bearerToken, USER_ROLES -> "internal")

  val mockAttributeRegistryManagement: AttributeRegistryManagementService = mock[AttributeRegistryManagementService]
  val mockTenantManagement: TenantManagementService                       = mock[TenantManagementService]

  val mockUuidSupplier: UUIDSupplier               = mock[UUIDSupplier]
  val mockDateTimeSupplier: OffsetDateTimeSupplier = mock[OffsetDateTimeSupplier]

  val tenantService: TenantApiService =
    TenantApiServiceImpl(mockAttributeRegistryManagement, mockTenantManagement, mockUuidSupplier, mockDateTimeSupplier)(
      ExecutionContext.global
    )

  def mockGetTenantById(tenantId: UUID, result: Tenant)(implicit contexts: Seq[(String, String)]) =
    (mockTenantManagement
      .getTenant(_: UUID)(_: Seq[(String, String)]))
      .expects(tenantId, contexts)
      .once()
      .returns(Future.successful(result.copy(id = tenantId)))

  def mockGetTenantByExternalId(externalId: ExternalId, result: Tenant)(implicit contexts: Seq[(String, String)]) =
    (mockTenantManagement
      .getTenantByExternalId(_: ExternalId)(_: Seq[(String, String)]))
      .expects(externalId, contexts)
      .once()
      .returns(Future.successful(result.copy(externalId = externalId)))

  def mockGetTenantByExternalIdNotFound(externalId: ExternalId)(implicit contexts: Seq[(String, String)]) =
    (mockTenantManagement
      .getTenantByExternalId(_: ExternalId)(_: Seq[(String, String)]))
      .expects(externalId, contexts)
      .once()
      .returns(Future.failed(ApiError(code = 404, message = "Not Found", responseContent = None)))

  def mockCreateTenant(seed: TenantSeed, result: Tenant)(implicit contexts: Seq[(String, String)]) =
    (mockTenantManagement
      .createTenant(_: TenantSeed)(_: Seq[(String, String)]))
      .expects(seed, contexts)
      .once()
      .returns(Future.successful(result.copy(externalId = seed.externalId)))

  def mockUpdateTenant(tenantId: UUID, payload: TenantDelta, result: Tenant = dependencyTenant)(implicit
    contexts: Seq[(String, String)]
  ) =
    (mockTenantManagement
      .updateTenant(_: UUID, _: TenantDelta)(_: Seq[(String, String)]))
      .expects(tenantId, payload, contexts)
      .once()
      .returns(Future.successful(result))

  def mockAddTenantAttribute(tenantId: UUID, attribute: TenantAttribute)(implicit contexts: Seq[(String, String)]) =
    (mockTenantManagement
      .addTenantAttribute(_: UUID, _: TenantAttribute)(_: Seq[(String, String)]))
      .expects(tenantId, attribute, contexts)
      .once()
      .returns(Future.successful(dependencyTenant))

  def mockGetAttributeByExternalId(origin: String, value: String, result: Attribute)(implicit
    contexts: Seq[(String, String)]
  ) =
    (mockAttributeRegistryManagement
      .getAttributeByExternalCode(_: String, _: String)(_: Seq[(String, String)]))
      .expects(origin, value, contexts)
      .once()
      .returns(Future.successful(result.copy(origin = Some(origin), code = Some(value))))

  def mockDateTimeGet() = (() => mockDateTimeSupplier.get).expects().returning(timestamp).once()

  def mockUuidGet(uuid: UUID) = (() => mockUuidSupplier.get).expects().returning(uuid).once()

}
