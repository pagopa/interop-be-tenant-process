package it.pagopa.interop.tenantprocess.utils

import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.tenantmanagement.client.model._
import it.pagopa.interop.tenantprocess.common.readmodel.PaginatedResult
import it.pagopa.interop.tenantmanagement.model.tenant.{
  PersistentCertifiedAttribute,
  PersistentExternalId,
  PersistentTenant,
  PersistentTenantAttribute,
  PersistentTenantFeature,
  PersistentTenantKind
}
import it.pagopa.interop.attributeregistrymanagement.model.persistence.attribute.{Certified, PersistentAttribute}
import it.pagopa.interop.catalogmanagement.model.CatalogItem
import it.pagopa.interop.agreementmanagement.model.agreement.{PersistentAgreement, PersistentAgreementState}
import it.pagopa.interop.agreementprocess.client.model.CompactTenant
import it.pagopa.interop.tenantprocess.service._
import org.mongodb.scala.bson.conversions.Bson
import spray.json.JsonReader

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object FakeDependencies extends SpecData {
  val verifiedAttributeId: UUID = UUID.randomUUID()
  val (agreement, eService)     = matchingAgreementAndEService(verifiedAttributeId)

  case class FakeAttributeRegistryManagement() extends AttributeRegistryManagementService {

    override def getAttributeById(
      id: UUID
    )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PersistentAttribute] =
      Future.successful(
        PersistentAttribute(
          id = UUID.randomUUID(),
          code = Some(UUID.randomUUID().toString),
          kind = Certified,
          description = "Attribute x",
          origin = Some("IPA"),
          name = "AttributeX",
          creationTime = OffsetDateTime.now()
        )
      )

    override def getAttributeByExternalCode(origin: String, code: String)(implicit
      ec: ExecutionContext,
      readModel: ReadModelService
    ): Future[PersistentAttribute] = Future.successful(
      PersistentAttribute(
        id = UUID.randomUUID(),
        code = Some(UUID.randomUUID().toString),
        kind = Certified,
        description = "Attribute x",
        origin = Some("IPA"),
        name = "AttributeX",
        creationTime = OffsetDateTime.now()
      )
    )
  }

  case class FakeTenantManagement() extends TenantManagementService {

    override def updateTenantAttribute(tenantId: UUID, attributeId: UUID, attribute: TenantAttribute)(implicit
      contexts: Seq[(String, String)]
    ): Future[Tenant] = Future.successful(fakeTenant)

    override def createTenant(seed: TenantSeed)(implicit contexts: Seq[(String, String)]): Future[Tenant] =
      Future.successful(fakeTenant)

    override def updateTenant(tenantId: UUID, payload: TenantDelta)(implicit
      contexts: Seq[(String, String)]
    ): Future[Tenant] = Future.successful(fakeTenant)

    override def addTenantAttribute(tenantId: UUID, seed: TenantAttribute)(implicit
      contexts: Seq[(String, String)]
    ): Future[Tenant] = Future.successful(fakeTenant)

    override def getTenantById(
      tenantId: UUID
    )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PersistentTenant] =
      Future.successful(fakePersistentTenant)

    override def getTenantByExternalId(
      externalId: PersistentExternalId
    )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PersistentTenant] =
      Future.successful(fakePersistentTenant)

    override def getTenantAttribute(tenantId: UUID, attributeId: UUID)(implicit
      ec: ExecutionContext,
      readModel: ReadModelService
    ): Future[PersistentTenantAttribute] = Future.successful(fakeAttribute)

    override def listConsumers(name: Option[String], producerId: UUID, offset: Int, limit: Int)(implicit
      ec: ExecutionContext,
      readModel: ReadModelService
    ): Future[PaginatedResult[PersistentTenant]] =
      Future.successful(paginatedResults)

    override def getTenants(name: Option[String], offset: Int, limit: Int)(implicit
      ec: ExecutionContext,
      readModel: ReadModelService
    ): Future[PaginatedResult[PersistentTenant]] =
      Future.successful(paginatedResults)

    override def listProducers(name: Option[String], offset: Int, limit: Int)(implicit
      ec: ExecutionContext,
      readModel: ReadModelService
    ): Future[PaginatedResult[PersistentTenant]] =
      Future.successful(paginatedResults)

    override def getTenantBySelfcareId(
      selfcareId: UUID
    )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PersistentTenant] =
      Future.successful(fakePersistentTenant)
  }

  case class FakeAgreementProcess() extends AgreementProcessService {
    override def computeAgreementsByAttribute(attributeId: UUID, consumer: CompactTenant)(implicit
      contexts: Seq[(String, String)]
    ): Future[Unit] = Future.unit
  }

  case class FakeAgreementManagement() extends AgreementManagementService {
    override def getAgreements(producerId: UUID, consumerId: UUID, states: Seq[PersistentAgreementState])(implicit
      ec: ExecutionContext,
      readModel: ReadModelService
    ): Future[Seq[PersistentAgreement]] = Future.successful(Seq(agreement))
  }

  case class FakeCatalogManagement() extends CatalogManagementService {
    override def getEServiceById(
      eServiceId: UUID
    )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[CatalogItem] =
      Future.successful(eService)
  }

  class FakeReadModelService extends ReadModelService {
    override def findOne[T](collectionName: String, filter: Bson)(implicit
      evidence$1: JsonReader[T],
      ec: ExecutionContext
    ): Future[Option[T]] = Future.successful(None)
    override def find[T](collectionName: String, filter: Bson, offset: Int, limit: Int)(implicit
      evidence$2: JsonReader[T],
      ec: ExecutionContext
    ): Future[Seq[T]] = Future.successful(Nil)
    override def find[T](collectionName: String, filter: Bson, projection: Bson, offset: Int, limit: Int)(implicit
      evidence$3: JsonReader[T],
      ec: ExecutionContext
    ): Future[Seq[T]] = Future.successful(Nil)
    override def aggregate[T](
      collectionName: String,
      pipeline: Seq[Bson],
      offset: Int,
      limit: Int,
      allowDiskUse: Boolean
    )(implicit evidence$4: JsonReader[T], ec: ExecutionContext): Future[Seq[T]] = Future.successful(Nil)
    def aggregateRaw[T: JsonReader](
      collectionName: String,
      pipeline: Seq[Bson],
      offset: Int,
      limit: Int,
      allowDiskUse: Boolean
    )(implicit ec: ExecutionContext): Future[Seq[T]] = Future.successful(Nil)

    override def close(): Unit = ()
  }

  val fakeTenant: Tenant = Tenant(
    id = UUID.randomUUID(),
    selfcareId = None,
    externalId = ExternalId("IPA", "something"),
    features = Seq(TenantFeature(certifier = Some(Certifier("SOMETHING")))),
    attributes = Nil,
    createdAt = OffsetDateTime.now(),
    updatedAt = None,
    mails = Nil,
    name = "test_name"
  )

  val fakePersistentTenant: PersistentTenant = PersistentTenant(
    id = UUID.randomUUID(),
    selfcareId = None,
    externalId = PersistentExternalId("IPA", "something"),
    features = List(PersistentTenantFeature.PersistentCertifier(certifierId = "SOMETHING")),
    attributes = Nil,
    createdAt = OffsetDateTime.now(),
    updatedAt = None,
    mails = Nil,
    name = "test_name",
    kind = Some(PersistentTenantKind.PA)
  )

  val fakeAttribute: PersistentCertifiedAttribute =
    PersistentCertifiedAttribute(UUID.randomUUID(), OffsetDateTime.now(), None)
}
