package it.pagopa.interop.tenantprocess.api.adapters

import it.pagopa.interop.tenantmanagement.client.model.{TenantAttribute, TenantSeed, TenantKind}
import it.pagopa.interop.tenantprocess.api.adapters.ApiAdapters.ExternalIdWrapper
import it.pagopa.interop.tenantprocess.model.{InternalTenantSeed, M2MTenantSeed, SelfcareTenantSeed}

import java.util.UUID

trait AdaptableSeed[T] {
  def toDependencySeed(t: T, id: UUID, attributes: Seq[TenantAttribute], kind: TenantKind): TenantSeed
}

object AdaptableSeed {
  def toDependency[A](a: A, id: UUID, attributes: Seq[TenantAttribute], kind: TenantKind)(implicit
    seed: AdaptableSeed[A]
  ): TenantSeed =
    seed.toDependencySeed(a, id, attributes, kind)

  implicit val internalAdaptable: AdaptableSeed[InternalTenantSeed] = new AdaptableSeed[InternalTenantSeed] {
    def toDependencySeed(
      a: InternalTenantSeed,
      id: UUID,
      attributes: Seq[TenantAttribute],
      kind: TenantKind
    ): TenantSeed =
      TenantSeed(
        id = Some(id),
        externalId = a.externalId.toDependency,
        features = Nil,
        attributes = attributes,
        name = a.name,
        kind = kind
      )
  }

  implicit val m2mAdaptable: AdaptableSeed[M2MTenantSeed] = new AdaptableSeed[M2MTenantSeed] {
    def toDependencySeed(a: M2MTenantSeed, id: UUID, attributes: Seq[TenantAttribute], kind: TenantKind): TenantSeed =
      TenantSeed(
        id = Some(id),
        externalId = a.externalId.toDependency,
        features = Nil,
        attributes = attributes,
        name = a.name,
        kind = kind
      )
  }

  implicit val selfcareAdaptable: AdaptableSeed[SelfcareTenantSeed] = new AdaptableSeed[SelfcareTenantSeed] {
    def toDependencySeed(
      a: SelfcareTenantSeed,
      id: UUID,
      attributes: Seq[TenantAttribute],
      kind: TenantKind
    ): TenantSeed =
      TenantSeed(
        id = Some(id),
        externalId = a.externalId.toDependency,
        features = Nil,
        attributes = attributes,
        name = a.name,
        kind = kind
      )
  }
}
