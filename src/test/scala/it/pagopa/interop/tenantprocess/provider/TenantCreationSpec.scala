package it.pagopa.interop.tenantprocess.provider

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.implicits._
import it.pagopa.interop.commons.utils.USER_ROLES
import it.pagopa.interop.tenantmanagement.client.model._
import it.pagopa.interop.tenantprocess.api.adapters.ApiAdapters.ExternalIdWrapper
import it.pagopa.interop.tenantprocess.api.impl.TenantApiMarshallerImpl._
import it.pagopa.interop.tenantprocess.model.{InternalAttributeSeed, M2MAttributeSeed}
import it.pagopa.interop.tenantprocess.provider.TenantCreationSpec._
import it.pagopa.interop.tenantprocess.utils.SpecHelper
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime
import java.util.UUID

class TenantCreationSpec extends AnyWordSpecLike with SpecHelper with ScalatestRouteTest {

  "Internal request - Creation of a new tenant with kind IPA with attributes must succeed" in {

    implicit val context: Seq[(String, String)] = internalContext

    val tenantId     = UUID.randomUUID()
    val tenant       = dependencyTenant.copy(id = tenantId)
    val attributeId1 = UUID.randomUUID()
    val attributeId2 = UUID.randomUUID()

    val attributeSeed1 = InternalAttributeSeed(origin = "origin1", code = "code1")
    val attributeSeed2 = InternalAttributeSeed(origin = "origin2", code = "code2")

    val attribute1 =
      dependencyAttribute.copy(id = attributeId1, origin = attributeSeed1.origin.some, code = attributeSeed1.code.some)
    val attribute2 =
      dependencyAttribute.copy(id = attributeId2, origin = attributeSeed2.origin.some, code = attributeSeed2.code.some)

    val seed = internalTenantSeed.copy(certifiedAttributes = Seq(attributeSeed1, attributeSeed2))

    val expectedAttribute1 = dependencyTenantAttribute.copy(certified =
      CertifiedTenantAttribute(id = attributeId1, assignmentTimestamp = timestamp, revocationTimestamp = None).some
    )
    val expectedAttribute2 = expectedAttribute1.withNewId(attributeId2)

    val expectedTenantSeed = TenantSeed(
      id = Some(tenantId),
      externalId = seed.externalId.toDependency,
      features = Nil,
      attributes = Seq(expectedAttribute1, expectedAttribute2),
      name = "test_name",
      kind = TenantKind.PA
    )

    mockDateTimeGet()
    mockUuidGet(tenantId)

    mockGetTenantByExternalIdNotFound(seed.externalId.toDependency)

    mockGetAttributeByExternalId(attribute1.origin.get, attribute1.code.get, attribute1)
    mockGetAttributeByExternalId(attribute2.origin.get, attribute2.code.get, attribute2)
    mockCreateTenant(expectedTenantSeed, tenant)

    Get() ~> tenantService.internalUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.OK)
    }
  }

  "Internal request - Creation of a new tenant with kind GSP with attributes using code L37 must succeed" in {

    implicit val context: Seq[(String, String)] = internalContext

    val tenantId     = UUID.randomUUID()
    val tenant       = dependencyTenant.copy(id = tenantId)
    val attributeId1 = UUID.randomUUID()
    val attributeId2 = UUID.randomUUID()

    val attributeSeed1 = InternalAttributeSeed(origin = "IPA", code = "L37")
    val attributeSeed2 = InternalAttributeSeed(origin = "origin2", code = "code2")

    val attribute1 =
      dependencyAttribute.copy(id = attributeId1, origin = attributeSeed1.origin.some, code = attributeSeed1.code.some)
    val attribute2 =
      dependencyAttribute.copy(id = attributeId2, origin = attributeSeed2.origin.some, code = attributeSeed2.code.some)

    val seed = internalTenantSeed.copy(certifiedAttributes = Seq(attributeSeed1, attributeSeed2))

    val expectedAttribute1 = dependencyTenantAttribute.copy(certified =
      CertifiedTenantAttribute(id = attributeId1, assignmentTimestamp = timestamp, revocationTimestamp = None).some
    )
    val expectedAttribute2 = expectedAttribute1.withNewId(attributeId2)

    val expectedTenantSeed = TenantSeed(
      id = Some(tenantId),
      externalId = seed.externalId.toDependency,
      features = Nil,
      attributes = Seq(expectedAttribute1, expectedAttribute2),
      name = "test_name",
      kind = TenantKind.GSP
    )

    mockDateTimeGet()
    mockUuidGet(tenantId)

    mockGetTenantByExternalIdNotFound(seed.externalId.toDependency)

    mockGetAttributeByExternalId(attribute1.origin.get, attribute1.code.get, attribute1)
    mockGetAttributeByExternalId(attribute2.origin.get, attribute2.code.get, attribute2)
    mockCreateTenant(expectedTenantSeed, tenant)

    Get() ~> tenantService.internalUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.OK)
    }
  }

  "Internal request - Creation of a new tenant with kind GSP with attributes using code SAG must succeed" in {

    implicit val context: Seq[(String, String)] = internalContext

    val tenantId     = UUID.randomUUID()
    val tenant       = dependencyTenant.copy(id = tenantId)
    val attributeId1 = UUID.randomUUID()
    val attributeId2 = UUID.randomUUID()

    val attributeSeed1 = InternalAttributeSeed(origin = "IPA", code = "SAG")
    val attributeSeed2 = InternalAttributeSeed(origin = "origin2", code = "code2")

    val attribute1 =
      dependencyAttribute.copy(id = attributeId1, origin = attributeSeed1.origin.some, code = attributeSeed1.code.some)
    val attribute2 =
      dependencyAttribute.copy(id = attributeId2, origin = attributeSeed2.origin.some, code = attributeSeed2.code.some)

    val seed = internalTenantSeed.copy(certifiedAttributes = Seq(attributeSeed1, attributeSeed2))

    val expectedAttribute1 = dependencyTenantAttribute.copy(certified =
      CertifiedTenantAttribute(id = attributeId1, assignmentTimestamp = timestamp, revocationTimestamp = None).some
    )
    val expectedAttribute2 = expectedAttribute1.withNewId(attributeId2)

    val expectedTenantSeed = TenantSeed(
      id = Some(tenantId),
      externalId = seed.externalId.toDependency,
      features = Nil,
      attributes = Seq(expectedAttribute1, expectedAttribute2),
      name = "test_name",
      kind = TenantKind.GSP
    )

    mockDateTimeGet()
    mockUuidGet(tenantId)

    mockGetTenantByExternalIdNotFound(seed.externalId.toDependency)

    mockGetAttributeByExternalId(attribute1.origin.get, attribute1.code.get, attribute1)
    mockGetAttributeByExternalId(attribute2.origin.get, attribute2.code.get, attribute2)
    mockCreateTenant(expectedTenantSeed, tenant)

    Get() ~> tenantService.internalUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.OK)
    }
  }

  "Internal request - Creation of a new tenant with kind PRIVATE with attributes must succeed" in {

    implicit val context: Seq[(String, String)] = internalContext

    val tenantId     = UUID.randomUUID()
    val tenant       = dependencyTenantNotIPA.copy(id = tenantId)
    val attributeId1 = UUID.randomUUID()
    val attributeId2 = UUID.randomUUID()

    val attributeSeed1 = InternalAttributeSeed(origin = "origin1", code = "code1")
    val attributeSeed2 = InternalAttributeSeed(origin = "origin2", code = "code2")

    val attribute1 =
      dependencyAttribute.copy(id = attributeId1, origin = attributeSeed1.origin.some, code = attributeSeed1.code.some)
    val attribute2 =
      dependencyAttribute.copy(id = attributeId2, origin = attributeSeed2.origin.some, code = attributeSeed2.code.some)

    val seed = internalTenantSeedNotIpa.copy(certifiedAttributes = Seq(attributeSeed1, attributeSeed2))

    val expectedAttribute1 = dependencyTenantAttribute.copy(certified =
      CertifiedTenantAttribute(id = attributeId1, assignmentTimestamp = timestamp, revocationTimestamp = None).some
    )
    val expectedAttribute2 = expectedAttribute1.withNewId(attributeId2)

    val expectedTenantSeed = TenantSeed(
      id = Some(tenantId),
      externalId = seed.externalId.toDependency,
      features = Nil,
      attributes = Seq(expectedAttribute1, expectedAttribute2),
      name = "test_name",
      kind = TenantKind.PRIVATE
    )

    mockDateTimeGet()
    mockUuidGet(tenantId)

    mockGetTenantByExternalIdNotFound(seed.externalId.toDependency)

    mockGetAttributeByExternalId(attribute1.origin.get, attribute1.code.get, attribute1)
    mockGetAttributeByExternalId(attribute2.origin.get, attribute2.code.get, attribute2)
    mockCreateTenant(expectedTenantSeed, tenant)

    Get() ~> tenantService.internalUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.OK)
    }
  }

  "Internal request - Update of an existing tenant with new attributes must succeed" in {

    implicit val context: Seq[(String, String)] = internalContext

    val noChangesExistingAttributeId = UUID.randomUUID()
    val updatedExistingAttributeId   = UUID.randomUUID()
    val newAttributeId1              = UUID.randomUUID()
    val newAttributeId2              = UUID.randomUUID()

    val noChangesExistingAttributeSeed = InternalAttributeSeed(origin = "origin1", code = "code1")
    val updatedExistingAttributeSeed   = InternalAttributeSeed(origin = "origin4", code = "code4")
    val newAttributeSeed1              = InternalAttributeSeed(origin = "origin2", code = "code2")
    val newAttributeSeed2              = InternalAttributeSeed(origin = "origin3", code = "code3")

    val noChangesExistingAttribute =
      dependencyAttribute.copy(
        id = noChangesExistingAttributeId,
        origin = noChangesExistingAttributeSeed.origin.some,
        code = noChangesExistingAttributeSeed.code.some
      )
    val updatedExistingAttribute   =
      dependencyAttribute.copy(
        id = updatedExistingAttributeId,
        origin = updatedExistingAttributeSeed.origin.some,
        code = updatedExistingAttributeSeed.code.some
      )
    val newAttribute1              =
      dependencyAttribute.copy(
        id = newAttributeId1,
        origin = newAttributeSeed1.origin.some,
        code = newAttributeSeed1.code.some
      )
    val newAttribute2              =
      dependencyAttribute.copy(
        id = newAttributeId2,
        origin = newAttributeSeed2.origin.some,
        code = newAttributeSeed2.code.some
      )

    val existingTenant =
      dependencyTenant.copy(attributes =
        Seq(
          dependencyTenantAttribute.withNewId(noChangesExistingAttributeId).withRevocation(None),
          dependencyTenantAttribute.withNewId(updatedExistingAttributeId).withRevocation(Some(timestamp))
        )
      )

    val seed =
      internalTenantSeed.copy(certifiedAttributes =
        Seq(noChangesExistingAttributeSeed, updatedExistingAttributeSeed, newAttributeSeed1, newAttributeSeed2)
      )

    val expectedNewAttribute1 = dependencyTenantAttribute.copy(certified =
      CertifiedTenantAttribute(id = newAttributeId1, assignmentTimestamp = timestamp, revocationTimestamp = None).some
    )
    val expectedNewAttribute2 = expectedNewAttribute1.withNewId(newAttributeId2)

    mockDateTimeGet()
    mockGetTenantByExternalId(seed.externalId.toDependency, existingTenant)

    mockGetAttributeByExternalId(
      noChangesExistingAttribute.origin.get,
      noChangesExistingAttribute.code.get,
      noChangesExistingAttribute
    )
    mockGetAttributeByExternalId(
      updatedExistingAttribute.origin.get,
      updatedExistingAttribute.code.get,
      updatedExistingAttribute
    )
    mockGetAttributeByExternalId(newAttribute1.origin.get, newAttribute1.code.get, newAttribute1)
    mockGetAttributeByExternalId(newAttribute2.origin.get, newAttribute2.code.get, newAttribute2)

    mockAddTenantAttribute(existingTenant.id, expectedNewAttribute1)
    mockAddTenantAttribute(existingTenant.id, expectedNewAttribute2)

    mockUpdateTenantAttribute(
      existingTenant.id,
      updatedExistingAttributeId,
      dependencyTenantAttribute.withNewId(updatedExistingAttributeId).withRevocation(None)
    )

    mockComputeAgreementState(existingTenant.id, newAttributeId1)
    mockComputeAgreementState(existingTenant.id, newAttributeId2)
    mockComputeAgreementState(existingTenant.id, updatedExistingAttributeId)

    mockGetTenantById(existingTenant.id, existingTenant)

    mockGetAttributeById(noChangesExistingAttributeId, noChangesExistingAttribute)
    mockGetAttributeById(updatedExistingAttributeId, updatedExistingAttribute)

    val expectedTenantUpdate =
      TenantDelta(selfcareId = None, features = Nil, mails = Nil, kind = TenantKind.PA)

    mockUpdateTenant(existingTenant.id, expectedTenantUpdate)

    Get() ~> tenantService.internalUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.OK)
    }
  }

  "Internal request - Update of an existing tenant with new attributes changing Tenant kind must succeed" in {

    implicit val context: Seq[(String, String)] = internalContext

    val noChangesExistingAttributeId = UUID.randomUUID()
    val updatedExistingAttributeId   = UUID.randomUUID()
    val newAttributeId1              = UUID.randomUUID()
    val newAttributeId2              = UUID.randomUUID()

    val noChangesExistingAttributeSeed = InternalAttributeSeed(origin = "origin1", code = "code1")
    val updatedExistingAttributeSeed   = InternalAttributeSeed(origin = "IPA", code = "L37")
    val newAttributeSeed1              = InternalAttributeSeed(origin = "origin2", code = "code2")
    val newAttributeSeed2              = InternalAttributeSeed(origin = "origin3", code = "code3")

    val noChangesExistingAttribute =
      dependencyAttribute.copy(
        id = noChangesExistingAttributeId,
        origin = noChangesExistingAttributeSeed.origin.some,
        code = noChangesExistingAttributeSeed.code.some
      )
    val updatedExistingAttribute   =
      dependencyAttribute.copy(
        id = updatedExistingAttributeId,
        origin = updatedExistingAttributeSeed.origin.some,
        code = updatedExistingAttributeSeed.code.some
      )
    val newAttribute1              =
      dependencyAttribute.copy(
        id = newAttributeId1,
        origin = newAttributeSeed1.origin.some,
        code = newAttributeSeed1.code.some
      )
    val newAttribute2              =
      dependencyAttribute.copy(
        id = newAttributeId2,
        origin = newAttributeSeed2.origin.some,
        code = newAttributeSeed2.code.some
      )

    val existingTenant =
      dependencyTenant.copy(attributes =
        Seq(
          dependencyTenantAttribute.withNewId(noChangesExistingAttributeId).withRevocation(None),
          dependencyTenantAttribute.withNewId(updatedExistingAttributeId).withRevocation(Some(timestamp))
        )
      )

    val seed =
      internalTenantSeed.copy(certifiedAttributes =
        Seq(noChangesExistingAttributeSeed, updatedExistingAttributeSeed, newAttributeSeed1, newAttributeSeed2)
      )

    val expectedNewAttribute1 = dependencyTenantAttribute.copy(certified =
      CertifiedTenantAttribute(id = newAttributeId1, assignmentTimestamp = timestamp, revocationTimestamp = None).some
    )
    val expectedNewAttribute2 = expectedNewAttribute1.withNewId(newAttributeId2)

    mockDateTimeGet()
    mockGetTenantByExternalId(seed.externalId.toDependency, existingTenant)

    mockGetAttributeByExternalId(
      noChangesExistingAttribute.origin.get,
      noChangesExistingAttribute.code.get,
      noChangesExistingAttribute
    )
    mockGetAttributeByExternalId(
      updatedExistingAttribute.origin.get,
      updatedExistingAttribute.code.get,
      updatedExistingAttribute
    )
    mockGetAttributeByExternalId(newAttribute1.origin.get, newAttribute1.code.get, newAttribute1)
    mockGetAttributeByExternalId(newAttribute2.origin.get, newAttribute2.code.get, newAttribute2)

    mockAddTenantAttribute(existingTenant.id, expectedNewAttribute1)
    mockAddTenantAttribute(existingTenant.id, expectedNewAttribute2)

    mockUpdateTenantAttribute(
      existingTenant.id,
      updatedExistingAttributeId,
      dependencyTenantAttribute.withNewId(updatedExistingAttributeId).withRevocation(None)
    )

    mockComputeAgreementState(existingTenant.id, newAttributeId1)
    mockComputeAgreementState(existingTenant.id, newAttributeId2)
    mockComputeAgreementState(existingTenant.id, updatedExistingAttributeId)

    mockGetTenantById(existingTenant.id, existingTenant)

    mockGetAttributeById(noChangesExistingAttributeId, noChangesExistingAttribute)
    mockGetAttributeById(updatedExistingAttributeId, updatedExistingAttribute)

    val expectedTenantUpdate =
      TenantDelta(selfcareId = None, features = Nil, mails = Nil, kind = TenantKind.GSP)

    mockUpdateTenant(existingTenant.id, expectedTenantUpdate)

    Get() ~> tenantService.internalUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.OK)
    }
  }

  "M2M request - Creation of a new tenant with Tenant Kind PA with attributes must succeed" in {

    implicit val context: Seq[(String, String)] = m2mContext

    val newTenantId       = UUID.randomUUID()
    val newTenant         = dependencyTenant.copy(id = newTenantId)
    val requesterTenantId = organizationId
    val requesterTenant   = dependencyTenant.copy(
      id = requesterTenantId,
      features = Seq(TenantFeature(certifier = Some(Certifier("CUSTOM_ORIGIN"))))
    )
    val attributeId1      = UUID.randomUUID()
    val attributeId2      = UUID.randomUUID()

    val attributeSeed1 = M2MAttributeSeed(code = "code1")
    val attributeSeed2 = M2MAttributeSeed(code = "code2")

    val attribute1 =
      dependencyAttribute.copy(id = attributeId1, origin = "CUSTOM_ORIGIN".some, code = attributeSeed1.code.some)
    val attribute2 =
      dependencyAttribute.copy(id = attributeId2, origin = "CUSTOM_ORIGIN".some, code = attributeSeed2.code.some)

    val seed = m2mTenantSeed.copy(certifiedAttributes = Seq(attributeSeed1, attributeSeed2))

    val expectedAttribute1 = dependencyTenantAttribute.copy(certified =
      CertifiedTenantAttribute(id = attributeId1, assignmentTimestamp = timestamp, revocationTimestamp = None).some
    )
    val expectedAttribute2 = expectedAttribute1.withNewId(attributeId2)

    val expectedTenantSeed = TenantSeed(
      id = Some(newTenantId),
      externalId = seed.externalId.toDependency,
      features = Nil,
      attributes = Seq(expectedAttribute1, expectedAttribute2),
      name = "test_name",
      kind = TenantKind.PA
    )

    mockDateTimeGet()
    mockUuidGet(newTenantId)

    mockGetTenantById(requesterTenantId, requesterTenant)
    mockGetTenantByExternalIdNotFound(seed.externalId.toDependency)

    mockGetAttributeByExternalId(attribute1.origin.get, attribute1.code.get, attribute1)
    mockGetAttributeByExternalId(attribute2.origin.get, attribute2.code.get, attribute2)

    mockCreateTenant(expectedTenantSeed, newTenant)

    Get() ~> tenantService.m2mUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.OK)
    }
  }

  "M2M request - Creation of a new tenant with Tenant Kind GSP with attributes must succeed" in {

    implicit val context: Seq[(String, String)] = m2mContext

    val newTenantId       = UUID.randomUUID()
    val newTenant         = dependencyTenant.copy(id = newTenantId)
    val requesterTenantId = organizationId
    val requesterTenant   =
      dependencyTenant.copy(id = requesterTenantId, features = Seq(TenantFeature(certifier = Some(Certifier("IPA")))))
    val attributeId1      = UUID.randomUUID()
    val attributeId2      = UUID.randomUUID()

    val attributeSeed1 = M2MAttributeSeed(code = "L37")
    val attributeSeed2 = M2MAttributeSeed(code = "code2")

    val attribute1 =
      dependencyAttribute.copy(id = attributeId1, origin = "IPA".some, code = attributeSeed1.code.some)
    val attribute2 =
      dependencyAttribute.copy(id = attributeId2, origin = "IPA".some, code = attributeSeed2.code.some)

    val seed = m2mTenantSeed.copy(certifiedAttributes = Seq(attributeSeed1, attributeSeed2))

    val expectedAttribute1 = dependencyTenantAttribute.copy(certified =
      CertifiedTenantAttribute(id = attributeId1, assignmentTimestamp = timestamp, revocationTimestamp = None).some
    )
    val expectedAttribute2 = expectedAttribute1.withNewId(attributeId2)

    val expectedTenantSeed = TenantSeed(
      id = Some(newTenantId),
      externalId = seed.externalId.toDependency,
      features = Nil,
      attributes = Seq(expectedAttribute1, expectedAttribute2),
      name = "test_name",
      kind = TenantKind.GSP
    )

    mockDateTimeGet()
    mockUuidGet(newTenantId)

    mockGetTenantById(requesterTenantId, requesterTenant)
    mockGetTenantByExternalIdNotFound(seed.externalId.toDependency)

    mockGetAttributeByExternalId(attribute1.origin.get, attribute1.code.get, attribute1)
    mockGetAttributeByExternalId(attribute2.origin.get, attribute2.code.get, attribute2)

    mockCreateTenant(expectedTenantSeed, newTenant)

    Get() ~> tenantService.m2mUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.OK)
    }
  }

  "M2M request - Creation of a new tenant with Tenant Kind PRIVATE with attributes must succeed" in {

    implicit val context: Seq[(String, String)] = m2mContext

    val newTenantId       = UUID.randomUUID()
    val newTenant         = dependencyTenantNotIPA.copy(id = newTenantId)
    val requesterTenantId = organizationId
    val requesterTenant   = dependencyTenant.copy(
      id = requesterTenantId,
      features = Seq(TenantFeature(certifier = Some(Certifier("CUSTOM_ORIGIN"))))
    )
    val attributeId1      = UUID.randomUUID()
    val attributeId2      = UUID.randomUUID()

    val attributeSeed1 = M2MAttributeSeed(code = "code1")
    val attributeSeed2 = M2MAttributeSeed(code = "code2")

    val attribute1 =
      dependencyAttribute.copy(id = attributeId1, origin = "CUSTOM_ORIGIN".some, code = attributeSeed1.code.some)
    val attribute2 =
      dependencyAttribute.copy(id = attributeId2, origin = "CUSTOM_ORIGIN".some, code = attributeSeed2.code.some)

    val seed = m2mTenantSeedNotIpa.copy(certifiedAttributes = Seq(attributeSeed1, attributeSeed2))

    val expectedAttribute1 = dependencyTenantAttribute.copy(certified =
      CertifiedTenantAttribute(id = attributeId1, assignmentTimestamp = timestamp, revocationTimestamp = None).some
    )
    val expectedAttribute2 = expectedAttribute1.withNewId(attributeId2)

    val expectedTenantSeed = TenantSeed(
      id = Some(newTenantId),
      externalId = seed.externalId.toDependency,
      features = Nil,
      attributes = Seq(expectedAttribute1, expectedAttribute2),
      name = "test_name",
      kind = TenantKind.PRIVATE
    )

    mockDateTimeGet()
    mockUuidGet(newTenantId)

    mockGetTenantById(requesterTenantId, requesterTenant)
    mockGetTenantByExternalIdNotFound(seed.externalId.toDependency)

    mockGetAttributeByExternalId(attribute1.origin.get, attribute1.code.get, attribute1)
    mockGetAttributeByExternalId(attribute2.origin.get, attribute2.code.get, attribute2)

    mockCreateTenant(expectedTenantSeed, newTenant)

    Get() ~> tenantService.m2mUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.OK)
    }
  }

  "M2M request - Revocation of an attribute in a tenant must succeed" in {

    implicit val context: Seq[(String, String)] = m2mContext

    val requesterTenantId = organizationId
    val certifierId       = "CUSTOM_ORIGIN"
    val code              = "code"
    val requesterTenant   = dependencyTenant.copy(
      id = requesterTenantId,
      features = TenantFeature(certifier = Certifier(certifierId).some) :: Nil
    )
    val origin            = "origin"
    val externalId        = "externalId"

    mockGetTenantById(requesterTenantId, requesterTenant)

    val attributeId     = UUID.randomUUID()
    val tenantAttribute =
      TenantAttribute(certified = CertifiedTenantAttribute(attributeId, OffsetDateTime.now, timestamp.some).some)

    val tenantToModify = dependencyTenant.copy(attributes = tenantAttribute :: Nil)

    val attribute = dependencyAttribute.copy(id = attributeId, origin = certifierId.some, code = code.some)

    mockGetTenantByExternalId(ExternalId(origin, externalId), tenantToModify)
    mockGetAttributeByExternalId(certifierId, code, attribute)
    mockDateTimeGet()
    mockUpdateTenantAttribute(dependencyTenant.id, attributeId, tenantAttribute)
    mockComputeAgreementState(dependencyTenant.id, attributeId)

    val expectedTenantUpdate =
      TenantDelta(selfcareId = None, features = Nil, mails = Nil, kind = TenantKind.PA)

    mockUpdateTenant(tenantToModify.id, expectedTenantUpdate)

    Get() ~> tenantService.m2mRevokeAttribute(origin, externalId, code) ~> check {
      assert(status == StatusCodes.NoContent)
    }
  }

  "M2M request - Revocation of an attribute in a tenant must fail if attribute is not found" in {

    implicit val context: Seq[(String, String)] = m2mContext

    val requesterTenantId = organizationId
    val certifierId       = "CUSTOM_ORIGIN"
    val code              = "code"
    val requesterTenant   = dependencyTenant.copy(
      id = requesterTenantId,
      features = TenantFeature(certifier = Certifier(certifierId).some) :: Nil
    )
    val origin            = "origin"
    val externalId        = "externalId"

    mockGetTenantById(requesterTenantId, requesterTenant)

    val attributeId     = UUID.randomUUID()
    val tenantAttribute =
      TenantAttribute(certified = CertifiedTenantAttribute(attributeId, OffsetDateTime.now, timestamp.some).some)

    val tenantToModify = dependencyTenant.copy(attributes = tenantAttribute :: Nil)

    mockGetTenantByExternalId(ExternalId(origin, externalId), tenantToModify)
    mockGetAttributeByExternalIdNotFound(certifierId, code)

    Get() ~> tenantService.m2mRevokeAttribute(origin, externalId, code) ~> check {
      assert(status == StatusCodes.NotFound)
    }
  }

  "M2M request - Revocation of an attribute in a tenant must fail if attribute doesn't correspond" in {

    implicit val context: Seq[(String, String)] = m2mContext

    val requesterTenantId = organizationId
    val certifierId       = "CUSTOM_ORIGIN"
    val code              = "code"
    val requesterTenant   = dependencyTenant.copy(
      id = requesterTenantId,
      features = TenantFeature(certifier = Certifier(certifierId).some) :: Nil
    )
    val origin            = "origin"
    val externalId        = "externalId"

    mockGetTenantById(requesterTenantId, requesterTenant)

    val attributeId = UUID.randomUUID()

    val tenantToModify = dependencyTenant.copy(attributes = Nil)

    val attribute = dependencyAttribute.copy(id = attributeId, origin = certifierId.some, code = code.some)

    mockGetTenantByExternalId(ExternalId(origin, externalId), tenantToModify)
    mockGetAttributeByExternalId(certifierId, code, attribute)

    Get() ~> tenantService.m2mRevokeAttribute(origin, externalId, code) ~> check {
      assert(status == StatusCodes.BadRequest)
    }
  }

  "M2M request - Revocation of an attribute in a tenant must fail if tenant is not a certifier" in {

    implicit val context: Seq[(String, String)] = m2mContext

    val requesterTenantId = organizationId
    val code              = "code"
    val requesterTenant   = dependencyTenant.copy(id = requesterTenantId, features = Nil)
    val origin            = "origin"
    val externalId        = "externalId"

    mockGetTenantById(requesterTenantId, requesterTenant)

    Get() ~> tenantService.m2mRevokeAttribute(origin, externalId, code) ~> check {
      assert(status == StatusCodes.Forbidden)
    }
  }

  "M2M request - Update of an existing tenant with new attributes must succeed" in {

    implicit val context: Seq[(String, String)] = m2mContext

    val requesterTenantId = organizationId
    val requesterTenant   = dependencyTenant.copy(
      id = requesterTenantId,
      features = Seq(TenantFeature(certifier = Some(Certifier("CUSTOM_ORIGIN"))))
    )

    val noChangesExistingAttributeId = UUID.randomUUID()
    val updatedExistingAttributeId   = UUID.randomUUID()
    val newAttributeId1              = UUID.randomUUID()
    val newAttributeId2              = UUID.randomUUID()

    val noChangesExistingAttributeSeed = M2MAttributeSeed(code = "code1")
    val newAttributeSeed1              = M2MAttributeSeed(code = "code2")
    val newAttributeSeed2              = M2MAttributeSeed(code = "code3")
    val updatedExistingAttributeSeed   = M2MAttributeSeed(code = "code4")

    val noChangesExistingAttribute =
      dependencyAttribute.copy(
        id = noChangesExistingAttributeId,
        origin = "CUSTOM_ORIGIN".some,
        code = noChangesExistingAttributeSeed.code.some
      )
    val updatedExistingAttribute   =
      dependencyAttribute.copy(
        id = updatedExistingAttributeId,
        origin = "CUSTOM_ORIGIN".some,
        code = updatedExistingAttributeSeed.code.some
      )
    val newAttribute1              =
      dependencyAttribute.copy(id = newAttributeId1, origin = "CUSTOM_ORIGIN".some, code = newAttributeSeed1.code.some)
    val newAttribute2              =
      dependencyAttribute.copy(id = newAttributeId2, origin = "CUSTOM_ORIGIN".some, code = newAttributeSeed2.code.some)

    val existingTenant =
      dependencyTenant.copy(attributes =
        Seq(
          dependencyTenantAttribute.withNewId(noChangesExistingAttributeId).withRevocation(None),
          dependencyTenantAttribute.withNewId(updatedExistingAttributeId).withRevocation(Some(timestamp))
        )
      )

    val seed =
      m2mTenantSeed.copy(certifiedAttributes =
        Seq(noChangesExistingAttributeSeed, updatedExistingAttributeSeed, newAttributeSeed1, newAttributeSeed2)
      )

    val expectedNewAttribute1 = dependencyTenantAttribute.copy(certified =
      CertifiedTenantAttribute(id = newAttributeId1, assignmentTimestamp = timestamp, revocationTimestamp = None).some
    )
    val expectedNewAttribute2 = expectedNewAttribute1.withNewId(newAttributeId2)

    mockDateTimeGet()

    mockGetTenantById(requesterTenantId, requesterTenant)
    mockGetTenantByExternalId(seed.externalId.toDependency, existingTenant)

    mockGetAttributeByExternalId(
      noChangesExistingAttribute.origin.get,
      noChangesExistingAttribute.code.get,
      noChangesExistingAttribute
    )
    mockGetAttributeByExternalId(
      updatedExistingAttribute.origin.get,
      updatedExistingAttribute.code.get,
      updatedExistingAttribute
    )
    mockGetAttributeByExternalId(newAttribute1.origin.get, newAttribute1.code.get, newAttribute1)
    mockGetAttributeByExternalId(newAttribute2.origin.get, newAttribute2.code.get, newAttribute2)

    mockAddTenantAttribute(existingTenant.id, expectedNewAttribute1)
    mockAddTenantAttribute(existingTenant.id, expectedNewAttribute2)

    mockUpdateTenantAttribute(
      existingTenant.id,
      updatedExistingAttributeId,
      dependencyTenantAttribute.withNewId(updatedExistingAttributeId).withRevocation(None)
    )

    mockComputeAgreementState(existingTenant.id, newAttributeId1)
    mockComputeAgreementState(existingTenant.id, newAttributeId2)
    mockComputeAgreementState(existingTenant.id, updatedExistingAttributeId)

    mockGetTenantById(existingTenant.id, existingTenant)

    mockGetAttributeById(noChangesExistingAttributeId, noChangesExistingAttribute)
    mockGetAttributeById(updatedExistingAttributeId, updatedExistingAttribute)

    val expectedTenantUpdate =
      TenantDelta(selfcareId = None, features = Nil, mails = Nil, kind = TenantKind.PA)

    mockUpdateTenant(existingTenant.id, expectedTenantUpdate)

    Get() ~> tenantService.m2mUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.OK)
    }
  }

  "M2M request - Must fail if requester is not a Certifier" in {

    implicit val context: Seq[(String, String)] = m2mContext

    val requesterTenantId = organizationId
    val requesterTenant   = dependencyTenant.copy(id = requesterTenantId, features = Nil)

    val attributeSeed = M2MAttributeSeed(code = "code1")

    val seed = m2mTenantSeed.copy(certifiedAttributes = Seq(attributeSeed))

    mockDateTimeGet()

    mockGetTenantById(requesterTenantId, requesterTenant)

    Get() ~> tenantService.m2mUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.Forbidden)
    }
  }

  "SelfCare request - Creation of a new tenant with kind PA must succeed" in {
    implicit val context: Seq[(String, String)] = selfcareContext

    val tenantId = UUID.randomUUID()
    val seed     = selfcareTenantSeed
    val tenant   = dependencyTenant.copy(id = tenantId)

    val expectedTenantSeed   =
      TenantSeed(
        id = Some(tenantId),
        externalId = seed.externalId.toDependency,
        features = Nil,
        attributes = Nil,
        name = "test_name",
        kind = TenantKind.PA
      )
    val expectedTenantUpdate =
      TenantDelta(selfcareId = Some(seed.selfcareId), features = Nil, mails = Nil, kind = TenantKind.PA)

    mockDateTimeGet()
    mockUuidGet(tenantId)

    mockGetTenantByExternalIdNotFound(seed.externalId.toDependency)

    mockCreateTenant(expectedTenantSeed, tenant)
    mockUpdateTenant(tenantId, expectedTenantUpdate)

    Get() ~> tenantService.selfcareUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.OK)
    }
  }

  "SelfCare request - Creation of a new tenant with kind PRIVATE must succeed" in {
    implicit val context: Seq[(String, String)] = selfcareContext

    val tenantId = UUID.randomUUID()
    val seed     = selfcareTenantSeedNotIpa
    val tenant   = dependencyTenant.copy(id = tenantId)

    val expectedTenantSeed   =
      TenantSeed(
        id = Some(tenantId),
        externalId = seed.externalId.toDependency,
        features = Nil,
        attributes = Nil,
        name = "test_name",
        kind = TenantKind.PRIVATE
      )
    val expectedTenantUpdate =
      TenantDelta(selfcareId = Some(seed.selfcareId), features = Nil, mails = Nil, kind = TenantKind.PRIVATE)

    mockDateTimeGet()
    mockUuidGet(tenantId)

    mockGetTenantByExternalIdNotFound(seed.externalId.toDependency)

    mockCreateTenant(expectedTenantSeed, tenant)
    mockUpdateTenant(tenantId, expectedTenantUpdate)

    Get() ~> tenantService.selfcareUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.OK)
    }
  }

  "SelfCare request - Update of an existing tenant must succeed if SelfCare ID is not set" in {
    implicit val context: Seq[(String, String)] = selfcareContext

    val tenantId = organizationId
    val seed     = selfcareTenantSeed
    val tenant   = dependencyTenant.copy(
      id = tenantId,
      selfcareId = None,
      features = Seq(TenantFeature(certifier = Some(Certifier("something"))))
    )

    val expectedTenantUpdate =
      TenantDelta(selfcareId = Some(seed.selfcareId), features = tenant.features, mails = Nil, kind = TenantKind.PA)

    mockDateTimeGet()

    mockGetTenantByExternalId(seed.externalId.toDependency, tenant)
    mockUpdateTenant(tenantId, expectedTenantUpdate)

    Get() ~> tenantService.selfcareUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.OK)
    }
  }

  "SelfCare request - Update should not be performed if existing SelfCare ID is equal to the request" in {
    implicit val context: Seq[(String, String)] = selfcareContext

    val tenantId   = organizationId
    val selfcareId = UUID.randomUUID().toString
    val seed       = selfcareTenantSeed.copy(selfcareId = selfcareId)
    val tenant     = dependencyTenant.copy(
      id = tenantId,
      selfcareId = Some(selfcareId),
      features = Seq(TenantFeature(certifier = Some(Certifier("something"))))
    )

    mockDateTimeGet()

    mockGetTenantByExternalId(seed.externalId.toDependency, tenant)

    Get() ~> tenantService.selfcareUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.OK)
    }
  }

  "SelfCare request - Must fail if existing SelfCare ID is different from request" in {
    implicit val context: Seq[(String, String)] = selfcareContext

    val tenantId           = organizationId
    val existingSelfcareId = UUID.randomUUID().toString
    val newSelfcareId      = UUID.randomUUID().toString
    val seed               = selfcareTenantSeed.copy(selfcareId = newSelfcareId)
    val tenant             = dependencyTenant.copy(
      id = tenantId,
      selfcareId = Some(existingSelfcareId),
      features = Seq(TenantFeature(certifier = Some(Certifier("something"))))
    )

    mockDateTimeGet()

    mockGetTenantByExternalId(seed.externalId.toDependency, tenant)

    Get() ~> tenantService.selfcareUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.Conflict)
    }
  }

  "SelfCare request - Must fail if requester does not belong to Tenant" in {
    implicit val context: Seq[(String, String)] = selfcareContext

    val tenantId           = UUID.randomUUID()
    val existingSelfcareId = UUID.randomUUID().toString
    val newSelfcareId      = UUID.randomUUID().toString
    val seed               = selfcareTenantSeed.copy(selfcareId = newSelfcareId)
    val tenant             = dependencyTenant.copy(
      id = tenantId,
      selfcareId = Some(existingSelfcareId),
      features = Seq(TenantFeature(certifier = Some(Certifier("something"))))
    )

    mockDateTimeGet()

    mockGetTenantByExternalId(seed.externalId.toDependency, tenant)

    Get() ~> tenantService.selfcareUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.Forbidden)
    }
  }

  "SelfCare request - Must succeed requested with Internal Role" in {
    implicit val context: Seq[(String, String)] = Seq("bearer" -> bearerToken, USER_ROLES -> "internal")

    val tenantId = UUID.randomUUID()
    val seed     = selfcareTenantSeed
    val tenant   = dependencyTenant.copy(
      id = tenantId,
      selfcareId = None,
      features = Seq(TenantFeature(certifier = Some(Certifier("something"))))
    )

    val expectedTenantUpdate =
      TenantDelta(selfcareId = Some(seed.selfcareId), features = tenant.features, mails = Nil, kind = TenantKind.PA)

    mockDateTimeGet()

    mockGetTenantByExternalId(seed.externalId.toDependency, tenant)
    mockUpdateTenant(tenantId, expectedTenantUpdate)

    Get() ~> tenantService.selfcareUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.OK)
    }
  }

}

object TenantCreationSpec {
  implicit class DependencyTenantAttributeSyntax(val a: TenantAttribute) extends AnyVal {
    def withNewId(id: UUID): TenantAttribute = a.copy(certified = a.certified.map(_.copy(id = id)))
    def withRevocation(timestamp: Option[OffsetDateTime]): TenantAttribute =
      a.copy(certified = a.certified.map(_.copy(revocationTimestamp = timestamp)))
  }
}
