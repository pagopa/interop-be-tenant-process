package it.pagopa.interop.tenantprocess.provider

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.implicits._
import it.pagopa.interop.tenantmanagement.client.model.{TenantAttributeKind, TenantSeed}
import it.pagopa.interop.tenantprocess.api.adapters.ApiAdapters.ExternalIdWrapper
import it.pagopa.interop.tenantprocess.api.impl.TenantApiMarshallerImpl._
import it.pagopa.interop.tenantprocess.model.InternalAttributeSeed
import it.pagopa.interop.tenantprocess.utils.SpecHelper
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class TenantCreationSpec extends AnyWordSpecLike with SpecHelper with ScalatestRouteTest {

  "Internal request - Update of an existing tenant with new attributes must succeed" in {

    implicit val context: Seq[(String, String)] = internalContext

    val existingAttributeId = UUID.randomUUID()
    val newAttributeId1     = UUID.randomUUID()
    val newAttributeId2     = UUID.randomUUID()

    val existingAttributeSeed = InternalAttributeSeed(origin = "origin1", code = "code1")
    val newAttributeSeed1     = InternalAttributeSeed(origin = "origin2", code = "code2")
    val newAttributeSeed2     = InternalAttributeSeed(origin = "origin3", code = "code3")

    val existingAttribute =
      dependencyAttribute.copy(
        id = existingAttributeId.toString,
        origin = existingAttributeSeed.origin.some,
        code = existingAttributeSeed.code.some
      )
    val newAttribute1     =
      dependencyAttribute.copy(
        id = newAttributeId1.toString,
        origin = newAttributeSeed1.origin.some,
        code = newAttributeSeed1.code.some
      )
    val newAttribute2     =
      dependencyAttribute.copy(
        id = newAttributeId2.toString,
        origin = newAttributeSeed2.origin.some,
        code = newAttributeSeed2.code.some
      )

    val existingTenant =
      dependencyTenant.copy(attributes = Seq(dependencyTenantAttribute.copy(id = existingAttributeId)))

    val seed =
      internalTenantSeed.copy(certifiedAttributes = Seq(existingAttributeSeed, newAttributeSeed1, newAttributeSeed2))

    val expectedNewAttribute1 = dependencyTenantAttribute.copy(
      id = newAttributeId1,
      kind = TenantAttributeKind.CERTIFIED,
      assignmentTimestamp = timestamp,
      revocationTimestamp = None,
      renewal = None,
      verifiedBy = None,
      revokedBy = None
    )
    val expectedNewAttribute2 = expectedNewAttribute1.copy(id = newAttributeId2)

    mockDateTimeGet()
    mockGetTenantByExternalId(internalTenantSeed.externalId.toDependency, existingTenant)

    mockGetAttributeByExternalId(existingAttribute.origin.get, existingAttribute.code.get, existingAttribute)
    mockGetAttributeByExternalId(newAttribute1.origin.get, newAttribute1.code.get, newAttribute1)
    mockGetAttributeByExternalId(newAttribute2.origin.get, newAttribute2.code.get, newAttribute2)

    mockAddTenantAttribute(dependencyTenant.id, expectedNewAttribute1)
    mockAddTenantAttribute(dependencyTenant.id, expectedNewAttribute2)

    Get() ~> tenantService.internalUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.Created)
    }
  }

  "Internal request - Creation of a new tenant with attributes must succeed" in {

    implicit val context: Seq[(String, String)] = internalContext

    val tenantId     = UUID.randomUUID()
    val attributeId1 = UUID.randomUUID()
    val attributeId2 = UUID.randomUUID()

    val attributeSeed1 = InternalAttributeSeed(origin = "origin1", code = "code1")
    val attributeSeed2 = InternalAttributeSeed(origin = "origin2", code = "code2")

    val attribute1 =
      dependencyAttribute.copy(
        id = attributeId1.toString,
        origin = attributeSeed1.origin.some,
        code = attributeSeed1.code.some
      )
    val attribute2 =
      dependencyAttribute.copy(
        id = attributeId2.toString,
        origin = attributeSeed2.origin.some,
        code = attributeSeed2.code.some
      )

    val seed =
      internalTenantSeed.copy(certifiedAttributes = Seq(attributeSeed1, attributeSeed2))

    val expectedAttribute1 = dependencyTenantAttribute.copy(
      id = attributeId1,
      kind = TenantAttributeKind.CERTIFIED,
      assignmentTimestamp = timestamp,
      revocationTimestamp = None,
      renewal = None,
      verifiedBy = None,
      revokedBy = None
    )
    val expectedAttribute2 = expectedAttribute1.copy(id = attributeId2)

    val expectedTenantSeed = TenantSeed(
      id = Some(tenantId),
      externalId = internalTenantSeed.externalId.toDependency,
      features = Nil,
      attributes = Seq(expectedAttribute1, expectedAttribute2)
    )

    mockDateTimeGet()
    mockUuidGet(tenantId)

    mockGetTenantByExternalIdNotFound(internalTenantSeed.externalId.toDependency)

    mockGetAttributeByExternalId(attribute1.origin.get, attribute1.code.get, attribute1)
    mockGetAttributeByExternalId(attribute2.origin.get, attribute2.code.get, attribute2)

    mockCreateTenant(expectedTenantSeed)

    Get() ~> tenantService.internalUpsertTenant(seed) ~> check {
      assert(status == StatusCodes.Created)
    }
  }

}