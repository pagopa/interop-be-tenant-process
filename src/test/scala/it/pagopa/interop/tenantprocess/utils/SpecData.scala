package it.pagopa.interop.tenantprocess.utils

import it.pagopa.interop.tenantprocess.model.{
  ExternalId,
  InternalAttributeSeed,
  InternalTenantSeed,
  M2MAttributeSeed,
  M2MTenantSeed,
  SelfcareTenantSeed
}

import java.util.UUID

object SpecData {
  val internalAttributeSeed: InternalAttributeSeed = InternalAttributeSeed("IPA", s"int-attribute-${UUID.randomUUID()}")
  val m2mAttributeSeed: M2MAttributeSeed           = M2MAttributeSeed(s"m2m-attribute-${UUID.randomUUID()}")

  val internalTenantSeed: InternalTenantSeed =
    InternalTenantSeed(ExternalId("IPA", s"tenant-${UUID.randomUUID()}"), Seq(internalAttributeSeed))
  val m2mTenantSeed: M2MTenantSeed           =
    M2MTenantSeed(ExternalId("IPA", s"tenant-${UUID.randomUUID()}"), Seq(m2mAttributeSeed))
  val selfcareTenantSeed: SelfcareTenantSeed =
    SelfcareTenantSeed(ExternalId("IPA", s"tenant-${UUID.randomUUID()}"), UUID.randomUUID().toString)
}
