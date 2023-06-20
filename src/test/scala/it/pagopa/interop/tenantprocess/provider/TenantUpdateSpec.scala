package it.pagopa.interop.tenantprocess.provider

import cats.syntax.all._
import akka.http.scaladsl.model.StatusCodes
import it.pagopa.interop.tenantprocess.api.impl.TenantApiMarshallerImpl._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.commons.utils.ORGANIZATION_ID_CLAIM
import it.pagopa.interop.tenantprocess.utils.SpecHelper
import org.scalatest.wordspec.AnyWordSpecLike
import it.pagopa.interop.tenantprocess.api.adapters.ApiAdapters._
import it.pagopa.interop.tenantprocess.api.adapters.TenantManagementAdapters._
import it.pagopa.interop.tenantprocess.api.adapters.ReadModelTenantAdapters._

import java.util.UUID
import it.pagopa.interop.tenantprocess.model.TenantDelta
import it.pagopa.interop.tenantprocess.model.Tenant
import it.pagopa.interop.tenantmanagement.client.{model => Dependency}
import it.pagopa.interop.tenantprocess.model.MailSeed
import it.pagopa.interop.tenantprocess.model.MailKind

import java.time.OffsetDateTime

class TenantUpdateSpec extends AnyWordSpecLike with SpecHelper with ScalatestRouteTest {

  "Tenant updated should not alter the tenant management behaviour when no mails" in {
    implicit val contexts: Seq[(String, String)] = adminContext

    val tenantId: UUID           = organizationId
    val tenantDelta: TenantDelta = TenantDelta(mails = Nil)

    val dependencyTenant: Dependency.Tenant = persistentTenant.toManagement
    val expected: Tenant                    = dependencyTenant.toApi

    mockGetTenantById(tenantId, persistentTenant)
    mockUpdateTenant(
      tenantId,
      tenantDelta.fromAPI(dependencyTenant.selfcareId, dependencyTenant.features, kind = Dependency.TenantKind.PA),
      dependencyTenant
    )

    Post() ~> tenantService.updateTenant(tenantId.toString, tenantDelta) ~> check {
      assert(status == StatusCodes.OK)
      assert(entityAs[Tenant] == expected)
    }
  }

  "Tenant updated should not alter the tenant management behaviour when there are mails" in {
    implicit val contexts: Seq[(String, String)] = adminContext

    val tenantId: UUID           = organizationId
    val tenantDelta: TenantDelta = TenantDelta(mails =
      MailSeed(kind = MailKind.CONTACT_EMAIL, address = "foo@bar.com", description = "awe".some) :: Nil
    )

    val dependencyTenant: Dependency.Tenant = persistentTenant.toManagement.copy(mails =
      Dependency.Mail(
        kind = Dependency.MailKind.CONTACT_EMAIL,
        address = "foo@bar.com",
        createdAt = OffsetDateTime.now(),
        description = "awe".some
      ) :: Nil
    )
    val expected: Tenant                    = dependencyTenant.toApi

    mockGetTenantById(tenantId, persistentTenant)
    mockUpdateTenant(
      tenantId,
      tenantDelta.fromAPI(dependencyTenant.selfcareId, dependencyTenant.features, kind = Dependency.TenantKind.PA),
      dependencyTenant
    )

    Post() ~> tenantService.updateTenant(tenantId.toString, tenantDelta) ~> check {
      assert(status == StatusCodes.OK)
      assert(entityAs[Tenant] == expected)
    }
  }

  "Tenant updated should not be allowed be user not belonging to the Tenant" in {
    implicit val contexts: Seq[(String, String)] =
      adminContext.filter(_._1 != ORGANIZATION_ID_CLAIM) :+ ORGANIZATION_ID_CLAIM -> UUID.randomUUID().toString

    val tenantId: UUID           = UUID.randomUUID()
    val tenantDelta: TenantDelta = TenantDelta(mails = Nil)

    Post() ~> tenantService.updateTenant(tenantId.toString, tenantDelta) ~> check {
      assert(status == StatusCodes.Forbidden)
    }
  }
}
