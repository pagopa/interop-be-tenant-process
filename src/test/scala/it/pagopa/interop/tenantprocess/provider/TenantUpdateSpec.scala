package it.pagopa.interop.tenantprocess.provider

import akka.http.scaladsl.model.StatusCodes
import it.pagopa.interop.tenantprocess.api.impl.TenantApiMarshallerImpl._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.tenantprocess.utils.SpecHelper
import org.scalatest.wordspec.AnyWordSpecLike
import it.pagopa.interop.commons.utils.Digester.toSha256

import it.pagopa.interop.tenantprocess.model._
import it.pagopa.interop.tenantmanagement.client.{model => Dependency}
import java.util.UUID

class TenantUpdateSpec extends AnyWordSpecLike with SpecHelper with ScalatestRouteTest {

  "Add tenant mail" in {
    implicit val contexts: Seq[(String, String)] = adminContext

    val mailSeed: MailSeed                      = MailSeed(MailKind.CONTACT_EMAIL, "foo@bar.it", None)
    val dependencyMailSeed: Dependency.MailSeed =
      Dependency.MailSeed(toSha256("foo@bar.it".getBytes()), Dependency.MailKind.CONTACT_EMAIL, "foo@bar.it", None)

    mockAddTenantMail(organizationId, dependencyMailSeed)

    Post() ~> tenantService.addTenantMail(organizationId.toString, mailSeed) ~> check {
      assert(status == StatusCodes.NoContent)
    }
  }

  "Delete tenant mail" in {
    implicit val contexts: Seq[(String, String)] = adminContext

    val mailId: String = toSha256("foo@bar.it".getBytes())

    mockDeleteTenantMail(organizationId, mailId)

    Post() ~> tenantService.deleteTenantMail(organizationId.toString, mailId) ~> check {
      assert(status == StatusCodes.NoContent)
    }
  }

  "Add tenant mail fail if tenant is not the requester" in {
    implicit val contexts: Seq[(String, String)] = adminContext

    val mailSeed: MailSeed = MailSeed(MailKind.CONTACT_EMAIL, "foo@bar.it", None)

    Post() ~> tenantService.addTenantMail(UUID.randomUUID().toString(), mailSeed) ~> check {
      assert(status == StatusCodes.Forbidden)
    }
  }

  "Delete tenant mail fail if tenant is not the requester" in {
    implicit val contexts: Seq[(String, String)] = adminContext

    val mailId: String = toSha256("foo@bar.it".getBytes())

    Post() ~> tenantService.deleteTenantMail(UUID.randomUUID().toString(), mailId) ~> check {
      assert(status == StatusCodes.Forbidden)
    }
  }
}
