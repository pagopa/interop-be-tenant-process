package it.pagopa.interop.tenantprocess.api.adapters

import it.pagopa.interop.tenantmanagement.client.model.{TenantAttribute, TenantSeed}
import it.pagopa.interop.tenantprocess.api.adapters.ApiAdapters.ExternalIdWrapper
import it.pagopa.interop.tenantprocess.model.{InternalTenantSeed, M2MTenantSeed, SelfcareTenantSeed}

import java.util.UUID

trait AdaptableSeed[T] {
  def toDependencySeed(t: T, id: UUID, attributes: Seq[TenantAttribute]): TenantSeed
}

object AdaptableSeed {
  def toDependency[A](a: A, id: UUID, attributes: Seq[TenantAttribute])(implicit seed: AdaptableSeed[A]): TenantSeed =
    seed.toDependencySeed(a, id, attributes)

  implicit val internalAdaptable: AdaptableSeed[InternalTenantSeed] = new AdaptableSeed[InternalTenantSeed] {
    def toDependencySeed(a: InternalTenantSeed, id: UUID, attributes: Seq[TenantAttribute]): TenantSeed =
      TenantSeed(
        id = Some(id),
        externalId = a.externalId.toDependency,
        features = Nil,
        attributes = attributes,
        name = a.name
      )
  }

  implicit val m2mAdaptable: AdaptableSeed[M2MTenantSeed] = new AdaptableSeed[M2MTenantSeed] {
    def toDependencySeed(a: M2MTenantSeed, id: UUID, attributes: Seq[TenantAttribute]): TenantSeed =
      TenantSeed(
        id = Some(id),
        externalId = a.externalId.toDependency,
        features = Nil,
        attributes = attributes,
        name = a.name
      )
  }

  implicit val selfcareAdaptable: AdaptableSeed[SelfcareTenantSeed] = new AdaptableSeed[SelfcareTenantSeed] {
    def toDependencySeed(a: SelfcareTenantSeed, id: UUID, attributes: Seq[TenantAttribute]): TenantSeed =
      TenantSeed(
        id = Some(id),
        externalId = a.externalId.toDependency,
        features = Nil,
        attributes = attributes,
        name = a.name
      )
  }
}
