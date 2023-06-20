package it.pagopa.interop.tenantprocess.utils

import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.tenantmanagement.client.model._
import it.pagopa.interop.tenantprocess.service._
import org.mongodb.scala.bson.conversions.Bson
import spray.json.JsonReader

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object FakeDependencies extends SpecData {
  val verifiedAttributeId: UUID = UUID.randomUUID()
  val (agreement, eService)     = matchingAgreementAndEService(verifiedAttributeId)

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
  }

  case class FakeAgreementProcess() extends AgreementProcessService {
    override def computeAgreementsByAttribute(consumerId: UUID, attributeId: UUID)(implicit
      contexts: Seq[(String, String)]
    ): Future[Unit] = Future.unit
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
    override def aggregate[T](collectionName: String, pipeline: Seq[Bson], offset: Int, limit: Int)(implicit
      evidence$4: JsonReader[T],
      ec: ExecutionContext
    ): Future[Seq[T]] = Future.successful(Nil)
    def aggregateRaw[T: JsonReader](collectionName: String, pipeline: Seq[Bson], offset: Int, limit: Int)(implicit
      ec: ExecutionContext
    ): Future[Seq[T]] = Future.successful(Nil)

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
}
