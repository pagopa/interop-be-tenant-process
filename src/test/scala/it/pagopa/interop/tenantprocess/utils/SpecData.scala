package it.pagopa.interop.tenantprocess.utils

import it.pagopa.interop.tenantmanagement.client.{model => Dependency}
import it.pagopa.interop.attributeregistrymanagement.model.persistence.attribute._
import it.pagopa.interop.tenantmanagement.model.tenant.{
  PersistentExternalId,
  PersistentTenant,
  PersistentVerifiedAttribute,
  PersistentDeclaredAttribute,
  PersistentCertifiedAttribute,
  PersistentTenantVerifier,
  PersistentTenantRevoker,
  PersistentTenantAttribute
}
import it.pagopa.interop.tenantprocess.model._
import it.pagopa.interop.agreementmanagement.model.agreement.{
  PersistentAgreement,
  Active,
  PersistentStamps,
  PersistentVerifiedAttribute => AgreementPersistentVerifiedAttribute
}

import it.pagopa.interop.catalogmanagement.model.{
  Rest,
  CatalogAttributeValue,
  CatalogItem,
  CatalogAttributes,
  SingleAttribute,
  CatalogDescriptor,
  Published,
  Automatic
}

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier

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

  val dependencyTenant: Dependency.Tenant      = Dependency.Tenant(
    id = UUID.randomUUID(),
    selfcareId = None,
    externalId = Dependency.ExternalId("IPA", "org"),
    features = Nil,
    attributes = Nil,
    createdAt = timestamp,
    updatedAt = None,
    mails = Nil,
    name = "test_name",
    kind = None
  )
  val persistentAttribute: PersistentAttribute = PersistentAttribute(
    id = UUID.randomUUID(),
    origin = Some("origin"),
    code = Some("value"),
    kind = Certified,
    description = "description",
    name = "name",
    creationTime = OffsetDateTimeSupplier.get().minusDays(10)
  )

  val dependencyTenantAttribute: Dependency.TenantAttribute = Dependency.TenantAttribute(certified =
    Some(
      Dependency
        .CertifiedTenantAttribute(id = UUID.randomUUID(), assignmentTimestamp = timestamp, revocationTimestamp = None)
    )
  )

  val persistentTenant: PersistentTenant = PersistentTenant(
    id = UUID.randomUUID(),
    selfcareId = None,
    externalId = PersistentExternalId("IPA", "org"),
    features = Nil,
    attributes = Nil,
    createdAt = timestamp,
    updatedAt = None,
    mails = Nil,
    name = "test_name",
    kind = None
  )

  val persistentTenantNotIPA: PersistentTenant = PersistentTenant(
    id = UUID.randomUUID(),
    selfcareId = None,
    externalId = PersistentExternalId("NOT_IPA", "org"),
    features = Nil,
    attributes = Nil,
    createdAt = timestamp,
    updatedAt = None,
    mails = Nil,
    name = "test_name",
    kind = None
  )

  val fakeTenantDelta: TenantDelta = TenantDelta(mails = Nil)

  val persistentTenantVerifier: PersistentTenantVerifier = PersistentTenantVerifier(
    id = UUID.randomUUID(),
    verificationDate = timestamp,
    expirationDate = None,
    extensionDate = Some(timestamp)
  )

  val persistentTenantRevoker: PersistentTenantRevoker = PersistentTenantRevoker(
    id = UUID.randomUUID(),
    verificationDate = timestamp,
    expirationDate = None,
    extensionDate = None,
    revocationDate = timestamp
  )

  val persistentTenantAttribute: PersistentTenantAttribute =
    PersistentCertifiedAttribute(id = UUID.randomUUID(), assignmentTimestamp = timestamp, revocationTimestamp = None)

  val persistentCertifiedAttribute: PersistentCertifiedAttribute =
    PersistentCertifiedAttribute(id = UUID.randomUUID(), assignmentTimestamp = timestamp, revocationTimestamp = None)

  val persistentDeclaredAttribute: PersistentDeclaredAttribute =
    PersistentDeclaredAttribute(id = UUID.randomUUID(), assignmentTimestamp = timestamp, revocationTimestamp = None)

  val persistentVerifiedAttribute: PersistentVerifiedAttribute =
    PersistentVerifiedAttribute(
      id = UUID.randomUUID(),
      assignmentTimestamp = timestamp,
      verifiedBy = List(persistentTenantVerifier),
      revokedBy = List(persistentTenantRevoker)
    )

  def persistentAgreement(
    eServiceId: UUID = UUID.randomUUID(),
    descriptorId: UUID = UUID.randomUUID(),
    verifiedAttributeId: UUID = UUID.randomUUID()
  ): PersistentAgreement = PersistentAgreement(
    id = UUID.randomUUID(),
    eserviceId = eServiceId,
    descriptorId = descriptorId,
    producerId = UUID.randomUUID(),
    consumerId = UUID.randomUUID(),
    state = Active,
    verifiedAttributes = Seq(AgreementPersistentVerifiedAttribute(verifiedAttributeId)),
    certifiedAttributes = Nil,
    declaredAttributes = Nil,
    consumerDocuments = Nil,
    stamps = PersistentStamps(),
    rejectionReason = None,
    suspendedByConsumer = None,
    suspendedByProducer = None,
    suspendedByPlatform = None,
    createdAt = OffsetDateTime.now().minusDays(30),
    updatedAt = Some(OffsetDateTime.now().minusDays(10)),
    consumerNotes = None,
    suspendedAt = None,
    contract = None
  )

  def catalogItem(
    eServiceId: UUID = UUID.randomUUID(),
    descriptorId: UUID = UUID.randomUUID(),
    verifiedAttributeId: UUID = UUID.randomUUID()
  ): CatalogItem =
    CatalogItem(
      id = eServiceId,
      producerId = UUID.randomUUID(),
      name = "EService",
      description = "EService desc",
      technology = Rest,
      attributes = None,
      createdAt = OffsetDateTimeSupplier.get().minusDays(10),
      descriptors = CatalogDescriptor(
        id = descriptorId,
        description = None,
        interface = None,
        version = "1",
        audience = Nil,
        voucherLifespan = 0,
        dailyCallsPerConsumer = 0,
        dailyCallsTotal = 0,
        docs = Nil,
        state = Published,
        publishedAt = None,
        suspendedAt = None,
        deprecatedAt = None,
        archivedAt = None,
        agreementApprovalPolicy = Some(Automatic),
        serverUrls = Nil,
        createdAt = OffsetDateTimeSupplier.get().minusDays(10),
        attributes =
          CatalogAttributes(Nil, Nil, verified = Seq(SingleAttribute(CatalogAttributeValue(verifiedAttributeId, true))))
      ) :: Nil
    )

  def matchingAgreementAndEService(
    verifiedAttributeId: UUID = UUID.randomUUID()
  ): (PersistentAgreement, CatalogItem) = {
    val eServiceId   = UUID.randomUUID()
    val descriptorId = UUID.randomUUID()

    (
      persistentAgreement(eServiceId, descriptorId, verifiedAttributeId),
      catalogItem(eServiceId, descriptorId, verifiedAttributeId)
    )
  }
}
