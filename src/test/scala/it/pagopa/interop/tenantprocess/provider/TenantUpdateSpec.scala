package it.pagopa.interop.tenantprocess.provider

import akka.http.scaladsl.model.StatusCodes
import it.pagopa.interop.tenantprocess.api.impl.TenantApiMarshallerImpl._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.tenantprocess.utils.SpecHelper
import org.scalatest.wordspec.AnyWordSpecLike
import it.pagopa.interop.commons.utils.Digester.toSha256

import it.pagopa.interop.tenantprocess.model._
import it.pagopa.interop.tenantmanagement.client.{model => Dependency}

class TenantUpdateSpec extends AnyWordSpecLike with SpecHelper with ScalatestRouteTest {

  "Add tenant mail" in {
    implicit val contexts: Seq[(String, String)] = adminContext

    val mailSeed: MailSeed                      = MailSeed(MailKind.CONTACT_EMAIL, "foo@bar.it", None)
    val dependencyMailSeed: Dependency.MailSeed =
      Dependency.MailSeed(toSha256("foo@bar.it".getBytes()), Dependency.MailKind.CONTACT_EMAIL, "foo@bar.it", None)

    mockAddTenantMail(tenantId, dependencyMailSeed)

    Post() ~> tenantService.addTenantMail(tenantId.toString, mailSeed) ~> check {
      assert(status == StatusCodes.NoContent)
    }
  }

  "Delete tenant mail" in {
    implicit val contexts: Seq[(String, String)] = adminContext

    val mailId: String = toSha256("foo@bar.it".getBytes())

    mockDeleteTenantMail(tenantId, mailId)

    Post() ~> tenantService.deleteTenantMail(tenantId.toString, mailId) ~> check {
      assert(status == StatusCodes.NoContent)
    }
  }
}
