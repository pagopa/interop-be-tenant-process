package it.pagopa.interop.tenantprocess.utils

import it.pagopa.interop.agreementmanagement.client.model.{Agreement, AgreementState}
import it.pagopa.interop.attributeregistrymanagement.client.model.{Attribute, AttributeKind}
import it.pagopa.interop.catalogmanagement.client.model.EService
import it.pagopa.interop.tenantmanagement.client.model._
import it.pagopa.interop.tenantprocess.service._

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.Future

object FakeDependencies extends SpecData {
  val verifiedAttributeId: UUID = UUID.randomUUID()
  val (agreement, eService)     = matchingAgreementAndEService(verifiedAttributeId)

  case class FakeAttributeRegistryManagement() extends AttributeRegistryManagementService {

    override def getAttributeById(id: UUID)(implicit contexts: Seq[(String, String)]): Future[Attribute] =
      Future.successful(
        Attribute(
          id = UUID.randomUUID(),
          code = Some(UUID.randomUUID().toString),
          kind = AttributeKind.CERTIFIED,
          description = "Attribute x",
          origin = Some("IPA"),
          name = "AttributeX",
          creationTime = OffsetDateTime.now()
        )
      )

    override def getAttributeByExternalCode(origin: String, code: String)(implicit
      contexts: Seq[(String, String)]
    ): Future[Attribute] = Future.successful(
      Attribute(
        id = UUID.randomUUID(),
        code = Some(UUID.randomUUID().toString),
        kind = AttributeKind.CERTIFIED,
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

    override def getTenant(tenantId: UUID)(implicit contexts: Seq[(String, String)]): Future[Tenant] =
      Future.successful(fakeTenant)

    override def getTenantByExternalId(externalId: ExternalId)(implicit
      contexts: Seq[(String, String)]
    ): Future[Tenant] = Future.successful(fakeTenant)

    override def getTenantAttribute(tenantId: UUID, attributeId: UUID)(implicit
      contexts: Seq[(String, String)]
    ): Future[TenantAttribute] = Future.successful(fakeAttribute)
  }

  case class FakeAgreementProcess() extends AgreementProcessService {
    override def computeAgreementsByAttribute(consumerId: UUID, attributeId: UUID)(implicit
      contexts: Seq[(String, String)]
    ): Future[Unit] = Future.unit
  }

  case class FakeAgreementManagement() extends AgreementManagementService {
    override def getAgreements(producerId: UUID, consumerId: UUID, states: Seq[AgreementState])(implicit
      contexts: Seq[(String, String)]
    ): Future[Seq[Agreement]] = Future.successful(Seq(agreement))
  }

  case class FakeCatalogManagement() extends CatalogManagementService {
    override def getEServiceById(eServiceId: UUID)(implicit contexts: Seq[(String, String)]): Future[EService] =
      Future.successful(eService)
  }

  val fakeTenant: Tenant = Tenant(
    id = UUID.randomUUID(),
    selfcareId = None,
    externalId = ExternalId("IPA", "something"),
    features = Seq(TenantFeature(certifier = Some(Certifier("SOMETHING")))),
    attributes = Nil,
    createdAt = OffsetDateTime.now(),
    updatedAt = None
  )

  val fakeAttribute: TenantAttribute = TenantAttribute(None, None, None)
}
