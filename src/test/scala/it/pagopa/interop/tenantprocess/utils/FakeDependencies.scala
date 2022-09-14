package it.pagopa.interop.tenantprocess.utils

import it.pagopa.interop.attributeregistrymanagement.client.model.{Attribute, AttributeKind}
import it.pagopa.interop.tenantmanagement.client.model.{
  Certifier,
  ExternalId,
  Tenant,
  TenantAttribute,
  TenantDelta,
  TenantFeature,
  TenantSeed
}
import it.pagopa.interop.tenantprocess.service.{AttributeRegistryManagementService, TenantManagementService}

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.Future

object FakeDependencies {
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

    override def deleteTenantAttribute(tenantId: UUID, attributeId: UUID)(implicit
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
}
