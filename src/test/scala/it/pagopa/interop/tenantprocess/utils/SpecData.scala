package it.pagopa.interop.tenantprocess.utils

import cats.implicits._
import it.pagopa.interop.attributeregistrymanagement.client.model.{AttributeKind, Attribute => DependencyAttribute}
import it.pagopa.interop.tenantmanagement.client.model.{
  CertifiedTenantAttribute => DependencyCertifiedTenantAttribute,
  DeclaredTenantAttribute => DependencyDeclaredTenantAttribute,
  ExternalId => DependencyExternalId,
  Tenant => DependencyTenant,
  TenantAttribute => DependencyTenantAttribute,
  TenantRevoker => DependencyTenantRevoker,
  TenantVerifier => DependencyTenantVerifier,
  VerifiedTenantAttribute => DependencyVerifiedTenantAttribute
}
import it.pagopa.interop.tenantprocess.model._
import it.pagopa.interop.agreementmanagement.client.model.{
  Agreement => DependencyAgreement,
  AgreementState => DependencyAgreementState,
  VerifiedAttribute => DependencyVerifiedAttribute
}
import it.pagopa.interop.catalogmanagement.client.model.{
  Attribute => CatalogAttribute,
  AttributeValue => CatalogAttributeValue,
  Attributes => CatalogAttributes,
  EService => CatalogEService,
  EServiceTechnology => CatalogEServiceTechnology,
  EServiceDescriptor => CatalogDescriptor,
  EServiceDescriptorState => CatalogDescriptorState,
  AgreementApprovalPolicy
}

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID
import it.pagopa.interop.agreementmanagement.client.model.Stamps

trait SpecData {
  final val timestamp = OffsetDateTime.of(2022, 12, 31, 11, 22, 33, 44, ZoneOffset.UTC)

  val internalAttributeSeed: InternalAttributeSeed = InternalAttributeSeed("IPA", s"int-attribute-${UUID.randomUUID()}")
  val m2mAttributeSeed: M2MAttributeSeed           = M2MAttributeSeed(s"m2m-attribute-${UUID.randomUUID()}")

  val internalTenantSeed: InternalTenantSeed       =
    InternalTenantSeed(ExternalId("IPA", s"tenant-${UUID.randomUUID()}"), Seq(internalAttributeSeed), "test_name")
  val internalTenantSeedNotIpa: InternalTenantSeed =
    InternalTenantSeed(ExternalId("NOT_IPA", s"tenant-${UUID.randomUUID()}"), Seq(internalAttributeSeed), "test_name")
  val m2mTenantSeed: M2MTenantSeed                 =
    M2MTenantSeed(ExternalId("IPA", s"tenant-${UUID.randomUUID()}"), Seq(m2mAttributeSeed), "test_name")
  val m2mTenantSeedNotIpa: M2MTenantSeed           =
    M2MTenantSeed(ExternalId("NOT_IPA", s"tenant-${UUID.randomUUID()}"), Seq(m2mAttributeSeed), "test_name")
  val selfcareTenantSeed: SelfcareTenantSeed       =
    SelfcareTenantSeed(ExternalId("IPA", s"tenant-${UUID.randomUUID()}"), UUID.randomUUID().toString, "test_name")
  val selfcareTenantSeedNotIpa: SelfcareTenantSeed =
    SelfcareTenantSeed(ExternalId("NOT_IPA", s"tenant-${UUID.randomUUID()}"), UUID.randomUUID().toString, "test_name")

  val dependencyTenant: DependencyTenant = DependencyTenant(
    id = UUID.randomUUID(),
    selfcareId = None,
    externalId = DependencyExternalId("IPA", "org"),
    features = Nil,
    attributes = Nil,
    createdAt = timestamp,
    updatedAt = None,
    mails = Nil,
    name = "test_name",
    kind = None
  )

  val dependencyTenantNotIPA: DependencyTenant = DependencyTenant(
    id = UUID.randomUUID(),
    selfcareId = None,
    externalId = DependencyExternalId("NOT_IPA", "org"),
    features = Nil,
    attributes = Nil,
    createdAt = timestamp,
    updatedAt = None,
    mails = Nil,
    name = "test_name",
    kind = None
  )

  val fakeTenantDelta: TenantDelta = TenantDelta(mails = Nil)

  val tenantVerifier: DependencyTenantVerifier = DependencyTenantVerifier(
    id = UUID.randomUUID(),
    verificationDate = timestamp,
    expirationDate = None,
    extensionDate = timestamp.some
  )

  val tenantRevoker: DependencyTenantRevoker = DependencyTenantRevoker(
    id = UUID.randomUUID(),
    verificationDate = timestamp,
    expirationDate = None,
    extensionDate = None,
    revocationDate = timestamp
  )

  val dependencyTenantAttribute: DependencyTenantAttribute = DependencyTenantAttribute(certified =
    DependencyCertifiedTenantAttribute(
      id = UUID.randomUUID(),
      assignmentTimestamp = timestamp,
      revocationTimestamp = None
    ).some
  )

  val dependencyCertifiedTenantAttribute: DependencyTenantAttribute = dependencyTenantAttribute

  val dependencyDeclaredTenantAttribute: DependencyTenantAttribute = DependencyTenantAttribute(declared =
    DependencyDeclaredTenantAttribute(
      id = UUID.randomUUID(),
      assignmentTimestamp = timestamp,
      revocationTimestamp = None
    ).some
  )

  def dependencyVerifiedTenantAttribute(
    id: UUID = UUID.randomUUID(),
    verifiedBy: Seq[DependencyTenantVerifier] = Seq(tenantVerifier),
    revokedBy: Seq[DependencyTenantRevoker] = Seq(tenantRevoker),
    assignmentTimestamp: OffsetDateTime = timestamp
  ): DependencyTenantAttribute = DependencyTenantAttribute(verified =
    DependencyVerifiedTenantAttribute(
      id = id,
      assignmentTimestamp = assignmentTimestamp,
      verifiedBy = verifiedBy,
      revokedBy = revokedBy
    ).some
  )

  val dependencyAttribute: DependencyAttribute = DependencyAttribute(
    id = UUID.randomUUID(),
    code = None,
    kind = AttributeKind.CERTIFIED,
    description = "An attribute",
    origin = None,
    name = "AttributeX",
    creationTime = timestamp
  )

  def dependencyAgreement(
    eServiceId: UUID = UUID.randomUUID(),
    descriptorId: UUID = UUID.randomUUID(),
    verifiedAttributeId: UUID = UUID.randomUUID()
  ): DependencyAgreement = DependencyAgreement(
    id = UUID.randomUUID(),
    eserviceId = eServiceId,
    descriptorId = descriptorId,
    producerId = UUID.randomUUID(),
    consumerId = UUID.randomUUID(),
    state = DependencyAgreementState.ACTIVE,
    verifiedAttributes = Seq(DependencyVerifiedAttribute(verifiedAttributeId)),
    certifiedAttributes = Nil,
    declaredAttributes = Nil,
    consumerDocuments = Nil,
    createdAt = OffsetDateTime.now(),
    stamps = Stamps(),
    rejectionReason = None
  )

  def catalogEService(
    eServiceId: UUID = UUID.randomUUID(),
    descriptorId: UUID = UUID.randomUUID(),
    verifiedAttributeId: UUID = UUID.randomUUID()
  ): CatalogEService =
    CatalogEService(
      id = eServiceId,
      producerId = UUID.randomUUID(),
      name = "EService",
      description = "EService desc",
      technology = CatalogEServiceTechnology.REST,
      descriptors = CatalogDescriptor(
        id = descriptorId,
        version = "1",
        audience = Nil,
        voucherLifespan = 0,
        dailyCallsPerConsumer = 0,
        dailyCallsTotal = 0,
        docs = Nil,
        state = CatalogDescriptorState.PUBLISHED,
        agreementApprovalPolicy = AgreementApprovalPolicy.AUTOMATIC,
        serverUrls = Nil,
        attributes = CatalogAttributes(
          Nil,
          Nil,
          verified = Seq(CatalogAttribute(single = Some(CatalogAttributeValue(verifiedAttributeId, true))))
        )
      ) :: Nil
    )

  def matchingAgreementAndEService(
    verifiedAttributeId: UUID = UUID.randomUUID()
  ): (DependencyAgreement, CatalogEService) = {
    val eServiceId   = UUID.randomUUID()
    val descriptorId = UUID.randomUUID()
    (
      dependencyAgreement(eServiceId, descriptorId, verifiedAttributeId),
      catalogEService(eServiceId, descriptorId, verifiedAttributeId)
    )
  }

}
