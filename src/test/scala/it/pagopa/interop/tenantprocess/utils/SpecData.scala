package it.pagopa.interop.tenantprocess.utils

import cats.implicits._
import it.pagopa.interop.attributeregistrymanagement.client.model.{AttributeKind, Attribute => DependencyAttribute}
import it.pagopa.interop.tenantmanagement.client.model.{
  ExternalId => DependencyExternalId,
  Tenant => DependencyTenant,
  TenantAttribute => DependencyTenantAttribute,
  CertifiedTenantAttribute => DependencyCertifiedTenantAttribute
}
import it.pagopa.interop.tenantprocess.model.{
  ExternalId,
  InternalAttributeSeed,
  InternalTenantSeed,
  M2MAttributeSeed,
  M2MTenantSeed,
  SelfcareTenantSeed
}

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

trait SpecData {
  final val timestamp = OffsetDateTime.of(2022, 12, 31, 11, 22, 33, 44, ZoneOffset.UTC)

  val internalAttributeSeed: InternalAttributeSeed = InternalAttributeSeed("IPA", s"int-attribute-${UUID.randomUUID()}")
  val m2mAttributeSeed: M2MAttributeSeed           = M2MAttributeSeed(s"m2m-attribute-${UUID.randomUUID()}")

  val internalTenantSeed: InternalTenantSeed =
    InternalTenantSeed(ExternalId("IPA", s"tenant-${UUID.randomUUID()}"), Seq(internalAttributeSeed))
  val m2mTenantSeed: M2MTenantSeed           =
    M2MTenantSeed(ExternalId("IPA", s"tenant-${UUID.randomUUID()}"), Seq(m2mAttributeSeed))
  val selfcareTenantSeed: SelfcareTenantSeed =
    SelfcareTenantSeed(ExternalId("IPA", s"tenant-${UUID.randomUUID()}"), UUID.randomUUID().toString)

  val dependencyTenant: DependencyTenant = DependencyTenant(
    id = UUID.randomUUID(),
    selfcareId = None,
    externalId = DependencyExternalId("IPA", "org"),
    features = Nil,
    attributes = Nil,
    createdAt = timestamp,
    updatedAt = None
  )

  val dependencyTenantAttribute: DependencyTenantAttribute = DependencyTenantAttribute(certified =
    DependencyCertifiedTenantAttribute(
      id = UUID.randomUUID(),
      assignmentTimestamp = timestamp,
      revocationTimestamp = None
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
}
