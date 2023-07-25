package it.pagopa.interop.tenantprocess.utils

import it.pagopa.interop.agreementmanagement.model.agreement._
import it.pagopa.interop.agreementprocess.client.model.CompactTenant
import it.pagopa.interop.attributeregistrymanagement.model.persistence.attribute.PersistentAttribute
import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.utils._
import it.pagopa.interop.commons.utils.service.{OffsetDateTimeSupplier, UUIDSupplier}
import it.pagopa.interop.tenantmanagement.model.tenant.{
  PersistentExternalId,
  PersistentTenant,
  PersistentTenantAttribute
}
import it.pagopa.interop.tenantprocess.error.TenantProcessErrors.{
  RegistryAttributeNotFound,
  TenantAttributeNotFound,
  TenantNotFound
}
import it.pagopa.interop.tenantmanagement.client.model._
import it.pagopa.interop.tenantprocess.common.readmodel.PaginatedResult
import it.pagopa.interop.tenantprocess.api.TenantApiService
import it.pagopa.interop.tenantprocess.api.impl.TenantApiServiceImpl
import it.pagopa.interop.tenantprocess.service._
import org.scalamock.scalatest.MockFactory

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait SpecHelper extends MockFactory with SpecData {

  val bearerToken          = "token"
  val organizationId: UUID = UUID.randomUUID()

  val selfcareContext: Seq[(String, String)] =
    Seq(
      "bearer"              -> bearerToken,
      USER_ROLES            -> "admin",
      UID                   -> UUID.randomUUID().toString,
      ORGANIZATION_ID_CLAIM -> organizationId.toString
    )
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

  implicit val mockReadModel: ReadModelService     = mock[ReadModelService]
  val mockUuidSupplier: UUIDSupplier               = mock[UUIDSupplier]
  val mockDateTimeSupplier: OffsetDateTimeSupplier = mock[OffsetDateTimeSupplier]

  val tenantService: TenantApiService =
    TenantApiServiceImpl(
      mockAttributeRegistryManagement,
      mockTenantManagement,
      mockAgreementProcess,
      mockAgreementManagement,
      mockCatalogManagement,
      mockUuidSupplier,
      mockDateTimeSupplier
    )(ExecutionContext.global, mockReadModel)

  def mockGetTenantById(tenantId: UUID, result: PersistentTenant = persistentTenant) =
    (mockTenantManagement
      .getTenantById(_: UUID)(_: ExecutionContext, _: ReadModelService))
      .expects(tenantId, *, *)
      .once()
      .returns(Future.successful(result.copy(id = tenantId)))

  def mockGetTenantBySelfcareId(selfcareId: UUID, result: PersistentTenant = persistentTenant) =
    (mockTenantManagement
      .getTenantBySelfcareId(_: UUID)(_: ExecutionContext, _: ReadModelService))
      .expects(selfcareId, *, *)
      .once()
      .returns(Future.successful(result.copy(selfcareId = Some(selfcareId.toString))))

  def mockGetProducers(
    name: Option[String],
    offset: Int,
    limit: Int,
    result: Seq[PersistentTenant] = Seq(persistentTenant)
  ) =
    (mockTenantManagement
      .listProducers(_: Option[String], _: Int, _: Int)(_: ExecutionContext, _: ReadModelService))
      .expects(name, offset, limit, *, *)
      .once()
      .returns(Future.successful(PaginatedResult(results = result, totalCount = result.size)))

  def mockGetConsumers(
    name: Option[String],
    producerId: UUID,
    offset: Int,
    limit: Int,
    result: Seq[PersistentTenant] = Seq(persistentTenant)
  ) =
    (mockTenantManagement
      .listConsumers(_: Option[String], _: UUID, _: Int, _: Int)(_: ExecutionContext, _: ReadModelService))
      .expects(name, producerId, offset, limit, *, *)
      .once()
      .returns(Future.successful(PaginatedResult(results = result, totalCount = result.size)))

  def mockGetTenants(
    name: Option[String],
    offset: Int,
    limit: Int,
    result: Seq[PersistentTenant] = Seq(persistentTenant)
  ) =
    (mockTenantManagement
      .getTenants(_: Option[String], _: Int, _: Int)(_: ExecutionContext, _: ReadModelService))
      .expects(name, offset, limit, *, *)
      .once()
      .returns(Future.successful(PaginatedResult(results = result, totalCount = result.size)))

  def mockGetTenantByExternalId(externalId: PersistentExternalId, result: PersistentTenant) =
    (mockTenantManagement
      .getTenantByExternalId(_: PersistentExternalId)(_: ExecutionContext, _: ReadModelService))
      .expects(externalId, *, *)
      .once()
      .returns(Future.successful(result.copy(externalId = PersistentExternalId(externalId.origin, externalId.value))))

  def mockGetTenantByExternalIdNotFound(externalId: PersistentExternalId) =
    (mockTenantManagement
      .getTenantByExternalId(_: PersistentExternalId)(_: ExecutionContext, _: ReadModelService))
      .expects(externalId, *, *)
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

  def mockAddTenantAttribute(tenantId: UUID, attribute: TenantAttribute, result: Tenant = dependencyTenant)(implicit
    contexts: Seq[(String, String)]
  ) =
    (mockTenantManagement
      .addTenantAttribute(_: UUID, _: TenantAttribute)(_: Seq[(String, String)]))
      .expects(tenantId, attribute, contexts)
      .once()
      .returns(Future.successful(result.copy(id = tenantId)))

  def mockGetTenantAttribute(tenantId: UUID, attributeId: UUID, result: PersistentTenantAttribute) =
    (mockTenantManagement
      .getTenantAttribute(_: UUID, _: UUID)(_: ExecutionContext, _: ReadModelService))
      .expects(tenantId, attributeId, *, *)
      .once()
      .returns(Future.successful(result))

  def mockGetTenantAttributeNotFound(tenantId: UUID, attributeId: UUID) =
    (mockTenantManagement
      .getTenantAttribute(_: UUID, _: UUID)(_: ExecutionContext, _: ReadModelService))
      .expects(tenantId, attributeId, *, *)
      .once()
      .returns(Future.failed(TenantAttributeNotFound(tenantId, attributeId)))

  def mockUpdateTenantAttribute(
    tenantId: UUID,
    attributeId: UUID,
    attribute: TenantAttribute,
    result: Tenant = dependencyTenant
  )(implicit contexts: Seq[(String, String)]) =
    (mockTenantManagement
      .updateTenantAttribute(_: UUID, _: UUID, _: TenantAttribute)(_: Seq[(String, String)]))
      .expects(tenantId, attributeId, attribute, contexts)
      .once()
      .returns(Future.successful(result.copy(id = tenantId)))

  def mockGetAttributeByExternalId(origin: String, value: String, result: PersistentAttribute) =
    (mockAttributeRegistryManagement
      .getAttributeByExternalCode(_: String, _: String)(_: ExecutionContext, _: ReadModelService))
      .expects(origin, value, *, *)
      .once()
      .returns(Future.successful(result.copy(origin = Some(origin), code = Some(value))))

  def mockGetAttributeByExternalIdNotFound(origin: String, value: String) =
    (mockAttributeRegistryManagement
      .getAttributeByExternalCode(_: String, _: String)(_: ExecutionContext, _: ReadModelService))
      .expects(origin, value, *, *)
      .once()
      .returns(Future.failed(RegistryAttributeNotFound(origin, value)))

  def mockGetAttributeById(id: UUID, result: PersistentAttribute) =
    (mockAttributeRegistryManagement
      .getAttributeById(_: UUID)(_: ExecutionContext, _: ReadModelService))
      .expects(id, *, *)
      .once()
      .returns(Future.successful(result.copy(id = id)))

  def mockComputeAgreementState(attributeId: UUID, consumer: CompactTenant)(implicit contexts: Seq[(String, String)]) =
    (mockAgreementProcess
      .computeAgreementsByAttribute(_: UUID, _: CompactTenant)(_: Seq[(String, String)]))
      .expects(attributeId, consumer, contexts)
      .once()
      .returns(Future.unit)

  def mockGetAgreements(result: Seq[PersistentAgreement]) =
    (mockAgreementManagement
      .getAgreements(_: UUID, _: UUID, _: Seq[PersistentAgreementState])(_: ExecutionContext, _: ReadModelService))
      .expects(*, *, *, *, *)
      .once()
      .returns(Future.successful(result))

  def mockGetEServiceById(eServiceId: UUID, result: CatalogItem) =
    (mockCatalogManagement
      .getEServiceById(_: UUID)(_: ExecutionContext, _: ReadModelService))
      .expects(*, *, *)
      .once()
      .returns(Future.successful(result.copy(id = eServiceId)))

  def mockDateTimeGet() = (() => mockDateTimeSupplier.get()).expects().returning(timestamp).once()

  def mockUuidGet(uuid: UUID) = (() => mockUuidSupplier.get()).expects().returning(uuid).once()

}
