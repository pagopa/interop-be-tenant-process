package it.pagopa.interop.tenantprocess.api.adapters

import it.pagopa.interop.tenantmanagement.client.model.{ExternalId => DependencyExternalId}
import it.pagopa.interop.tenantprocess.model.ExternalId

object ApiAdapters {

//  implicit class InternalTenantSeedWrapper(private val t: InternalTenantSeed) extends AnyVal {
//    def toDependency(id: UUID, attributes: Seq[DependencyTenantAttribute]): DependencyTenantSeed =
//      DependencyTenantSeed(
//        id = Some(id),
//        selfcareId = UUID.randomUUID(), // TODO Remove this on dependency fix
//        externalId = t.externalId.toDependency,
//        kind = false,                   // TODO This should be STANDARD
//        attributes = attributes
//      )
//  }

  //  implicit class TenantSeedWrapper(private val t: TenantSeed) extends AnyVal {
//    def toDependency(id: UUID): DependencyTenantSeed = DependencyTenantSeed(
//      id = Some(id),
//      selfcareId = UUID.fromString(t.selfcareId), // TODO Update this on dependency fix
//      externalId = t.externalId.toDependency,
//      kind = t.kind,
//      attributes = t.attributes.map(_.toDependency)
//    )
//  }

  implicit class ExternalIdWrapper(private val id: ExternalId) extends AnyVal {
    def toDependency: DependencyExternalId = DependencyExternalId(origin = id.origin, value = id.value)
  }

//  implicit class TenantAttributeWrapper(private val t: TenantAttribute) extends AnyVal {
//    def toDependency: DependencyTenantAttribute = DependencyTenantAttribute(
//      id = t.id,
//      kind = t.kind.toDependency,
//      assignmentTimestamp = t.assignmentTimestamp,
//      revocationTimestamp = t.revocationTimestamp,
//      extensionTimestamp = t.extensionTimestamp,
//      expirationTimestamp = t.expirationTimestamp
//    )
//  }

//  implicit class TenantAttributeKindWrapper(private val t: TenantAttributeKind) extends AnyVal {
//    def toDependency: DependencyTenantAttributeKind = t match {
//      case TenantAttributeKind.CERTIFIED => DependencyTenantAttributeKind.CERTIFIED
//      case TenantAttributeKind.DECLARED  => DependencyTenantAttributeKind.DECLARED
//      case TenantAttributeKind.VERIFIED  => DependencyTenantAttributeKind.VERIFIED
//    }
//  }

}
