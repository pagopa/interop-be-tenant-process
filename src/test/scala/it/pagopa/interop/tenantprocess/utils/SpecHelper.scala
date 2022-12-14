package it.pagopa.interop.tenantprocess.utils

import it.pagopa.interop.agreementmanagement.client.model.{Agreement, AgreementState}
import it.pagopa.interop.attributeregistrymanagement.client.model.Attribute
import it.pagopa.interop.catalogmanagement.client.model.EService
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.utils._
import it.pagopa.interop.commons.utils.service.{OffsetDateTimeSupplier, UUIDSupplier}
import it.pagopa.interop.tenantmanagement.client.model._
import it.pagopa.interop.tenantprocess.api.TenantApiService
import it.pagopa.interop.tenantprocess.api.impl.TenantApiServiceImpl
import it.pagopa.interop.tenantprocess.error.TenantProcessErrors.{
  RegistryAttributeNotFound,
  TenantAttributeNotFound,
  TenantNotFound
}
import it.pagopa.interop.tenantprocess.service._
import org.scalamock.scalatest.MockFactory

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait SpecHelper extends MockFactory with SpecData {

  val bearerToken          = "token"
  val organizationId: UUID = UUID.randomUUID()

  val selfcareContext: Seq[(String, String)] =
    Seq("bearer" -> bearerToken, USER_ROLES -> "admin", UID -> UUID.randomUUID().toString)
  val m2mContext: Seq[(String, String)]      =
    Seq("bearer" -> bearerToken, USER_ROLES -> "m2m", ORGANIZATION_ID_CLAIM -> organizationId.toString)
  val internalContext: Seq[(String, String)] =
    Seq("bearer" -> bearerToken, USER_ROLES -> "internal")
  val adminContext: Seq[(String, String)]    =
    Seq("bearer" -> bearerToken, USER_ROLES -> "admin", ORGANIZATION_ID_CLAIM -> organizationId.toString)

  val mockAttributeRegistryManagement: AttributeRegistryManagementService = mock[AttributeRegistryManagementService]
  val mockTenantManagement: TenantManagementService                       = mock[TenantManagementService]
  val mockAgreementProcess: AgreementProcessService                       = mock[AgreementProcessService]
  val mockAgreementManagement: AgreementManagementService                 = mock[AgreementManagementService]
  val mockCatalogManagement: CatalogManagementService                     = mock[CatalogManagementService]

  val mockReadModel: ReadModelService              = new ReadModelService(
    ReadModelConfig(
      "mongodb://localhost/?socketTimeoutMS=1&serverSelectionTimeoutMS=1&connectTimeoutMS=1&&autoReconnect=false&keepAlive=false",
      "db"
    )
  )
  val mockUuidSupplier: UUIDSupplier               = mock[UUIDSupplier]
  val mockDateTimeSupplier: OffsetDateTimeSupplier = mock[OffsetDateTimeSupplier]

  val tenantService: TenantApiService =
    TenantApiServiceImpl(
      mockAttributeRegistryManagement,
      mockTenantManagement,
      mockAgreementProcess,
      mockAgreementManagement,
      mockCatalogManagement,
      mockReadModel,
      mockUuidSupplier,
      mockDateTimeSupplier
    )(ExecutionContext.global)

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
      .returns(Future.failed(TenantNotFound(externalId.origin, externalId.value)))

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

  def mockGetTenantAttribute(tenantId: UUID, attributeId: UUID, result: TenantAttribute = dependencyTenantAttribute)(
    implicit contexts: Seq[(String, String)]
  ) = (mockTenantManagement
    .getTenantAttribute(_: UUID, _: UUID)(_: Seq[(String, String)]))
    .expects(tenantId, attributeId, contexts)
    .once()
    .returns(Future.successful(result))

  def mockGetTenantAttributeNotFound(tenantId: UUID, attributeId: UUID)(implicit contexts: Seq[(String, String)]) =
    (mockTenantManagement
      .getTenantAttribute(_: UUID, _: UUID)(_: Seq[(String, String)]))
      .expects(tenantId, attributeId, contexts)
      .once()
      .returns(Future.failed(TenantAttributeNotFound(tenantId, attributeId)))

  def mockUpdateTenantAttribute(tenantId: UUID, attributeId: UUID, attribute: TenantAttribute)(implicit
    contexts: Seq[(String, String)]
  ) =
    (mockTenantManagement
      .updateTenantAttribute(_: UUID, _: UUID, _: TenantAttribute)(_: Seq[(String, String)]))
      .expects(tenantId, attributeId, attribute, contexts)
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

  def mockGetAttributeByExternalIdNotFound(origin: String, value: String)(implicit contexts: Seq[(String, String)]) =
    (mockAttributeRegistryManagement
      .getAttributeByExternalCode(_: String, _: String)(_: Seq[(String, String)]))
      .expects(origin, value, contexts)
      .once()
      .returns(Future.failed(RegistryAttributeNotFound(origin, value)))

  def mockGetAttributeById(id: UUID, result: Attribute)(implicit contexts: Seq[(String, String)]) =
    (mockAttributeRegistryManagement
      .getAttributeById(_: UUID)(_: Seq[(String, String)]))
      .expects(id, contexts)
      .once()
      .returns(Future.successful(result))

  def mockComputeAgreementState(consumerId: UUID, attributeId: UUID)(implicit contexts: Seq[(String, String)]) =
    (mockAgreementProcess
      .computeAgreementsByAttribute(_: UUID, _: UUID)(_: Seq[(String, String)]))
      .expects(consumerId, attributeId, contexts)
      .once()
      .returns(Future.unit)

  def mockGetAgreements(producerId: UUID, consumerId: UUID, states: Seq[AgreementState], result: Seq[Agreement])(
    implicit contexts: Seq[(String, String)]
  ) = (mockAgreementManagement
    .getAgreements(_: UUID, _: UUID, _: Seq[AgreementState])(_: Seq[(String, String)]))
    .expects(producerId, consumerId, states, contexts)
    .once()
    .returns(Future.successful(result))

  def mockGetEServiceById(eServiceId: UUID, result: EService)(implicit contexts: Seq[(String, String)]) =
    (mockCatalogManagement
      .getEServiceById(_: UUID)(_: Seq[(String, String)]))
      .expects(eServiceId, contexts)
      .once()
      .returns(Future.successful(result))

  def mockDateTimeGet() = (() => mockDateTimeSupplier.get()).expects().returning(timestamp).once()

  def mockUuidGet(uuid: UUID) = (() => mockUuidSupplier.get()).expects().returning(uuid).once()

}
