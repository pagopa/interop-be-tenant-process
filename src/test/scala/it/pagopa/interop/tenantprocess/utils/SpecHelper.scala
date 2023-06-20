package it.pagopa.interop.tenantprocess.utils

import it.pagopa.interop.agreementmanagement.model.agreement._
import it.pagopa.interop.attributeregistrymanagement.model.persistence.attribute.PersistentAttribute
import it.pagopa.interop.catalogmanagement.model._
import org.mongodb.scala.bson.conversions.Bson
import spray.json._
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.utils._
import it.pagopa.interop.commons.utils.service.{OffsetDateTimeSupplier, UUIDSupplier}
import it.pagopa.interop.tenantmanagement.model.tenant.{
  PersistentTenant,
  PersistentExternalId,
  PersistentTenantAttribute
}
import it.pagopa.interop.tenantmanagement.client.model._
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

  val mockTenantManagement: TenantManagementService = mock[TenantManagementService]
  val mockAgreementProcess: AgreementProcessService = mock[AgreementProcessService]

  val mockReadModel: ReadModelService              = mock[ReadModelService]
  val mockUuidSupplier: UUIDSupplier               = mock[UUIDSupplier]
  val mockDateTimeSupplier: OffsetDateTimeSupplier = mock[OffsetDateTimeSupplier]

  val tenantService: TenantApiService =
    TenantApiServiceImpl(
      mockTenantManagement,
      mockAgreementProcess,
      mockReadModel,
      mockUuidSupplier,
      mockDateTimeSupplier
    )(ExecutionContext.global)

  def mockGetTenantById(tenantId: UUID, result: PersistentTenant) =
    (mockReadModel
      .findOne(_: String, _: Bson)(_: JsonReader[_], _: ExecutionContext))
      .expects("tenants", *, *, *)
      .once()
      .returns(Future.successful(Some(result.copy(id = tenantId))))

  def mockGetTenantByExternalId(externalId: ExternalId, result: PersistentTenant) =
    (mockReadModel
      .findOne(_: String, _: Bson)(_: JsonReader[_], _: ExecutionContext))
      .expects("tenants", *, *, *)
      .once()
      .returns(
        Future.successful(Some(result.copy(externalId = PersistentExternalId(externalId.origin, externalId.value))))
      )

  def mockGetTenantByExternalIdNotFound() =
    (mockReadModel
      .findOne(_: String, _: Bson)(_: JsonReader[_], _: ExecutionContext))
      .expects("tenants", *, *, *)
      .once()
      .returns(Future.successful(None))

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

  def mockGetTenantAttribute(result: PersistentTenantAttribute = persistentCertifiedAttribute) = (mockReadModel
    .findOne(_: String, _: Bson)(_: JsonReader[_], _: ExecutionContext))
    .expects("tenants", *, *, *)
    .once()
    .returns(Future.successful(Some(result)))

  def mockUpdateTenantAttribute(tenantId: UUID, attributeId: UUID, attribute: TenantAttribute)(implicit
    contexts: Seq[(String, String)]
  ) =
    (mockTenantManagement
      .updateTenantAttribute(_: UUID, _: UUID, _: TenantAttribute)(_: Seq[(String, String)]))
      .expects(tenantId, attributeId, attribute, contexts)
      .once()
      .returns(Future.successful(dependencyTenant))

  def mockGetAttributeByExternalId(origin: String, value: String, result: PersistentAttribute) =
    (mockReadModel
      .findOne(_: String, _: Bson)(_: JsonReader[_], _: ExecutionContext))
      .expects("attributes", *, *, *)
      .once()
      .returns(Future.successful(Some(result.copy(origin = Some(origin), code = Some(value)))))

  def mockGetAttributeByExternalIdNotFound() =
    (mockReadModel
      .findOne(_: String, _: Bson)(_: JsonReader[_], _: ExecutionContext))
      .expects("attributes", *, *, *)
      .once()
      .returns(Future.successful(None))

  def mockGetAttributeById(id: UUID, result: PersistentAttribute) =
    (mockReadModel
      .findOne(_: String, _: Bson)(_: JsonReader[_], _: ExecutionContext))
      .expects("attributes", *, *, *)
      .once()
      .returns(Future.successful(Some(result.copy(id = id))))

  def mockComputeAgreementState(consumerId: UUID, attributeId: UUID)(implicit contexts: Seq[(String, String)]) =
    (mockAgreementProcess
      .computeAgreementsByAttribute(_: UUID, _: UUID)(_: Seq[(String, String)]))
      .expects(consumerId, attributeId, contexts)
      .once()
      .returns(Future.unit)

  def mockGetAgreements(result: Seq[PersistentAgreement]) =
    (mockReadModel
      .find[PersistentAgreement](_: String, _: Bson, _: Int, _: Int)(
        _: JsonReader[PersistentAgreement],
        _: ExecutionContext
      ))
      .expects("agreements", *, *, *, *, *)
      .once()
      .returns(Future.successful(result))

  def mockGetEServiceById(eServiceId: UUID, result: CatalogItem) =
    (mockReadModel
      .findOne(_: String, _: Bson)(_: JsonReader[_], _: ExecutionContext))
      .expects("eservices", *, *, *)
      .once()
      .returns(Future.successful(Some(result.copy(id = eServiceId))))

  def mockDateTimeGet() = (() => mockDateTimeSupplier.get()).expects().returning(timestamp).once()

  def mockUuidGet(uuid: UUID) = (() => mockUuidSupplier.get()).expects().returning(uuid).once()

}
