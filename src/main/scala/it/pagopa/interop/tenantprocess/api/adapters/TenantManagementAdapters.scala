package it.pagopa.interop.tenantprocess.api.adapters

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.interop.tenantmanagement.client.model.{
  Problem => DependencyProblem,
  ProblemError => DependencyProblemError,
  Tenant => DependencyTenant
}
import it.pagopa.interop.tenantprocess.model.{Problem, ProblemError, Tenant}
import spray.json._

import scala.util.Try

object TenantManagementAdapters extends SprayJsonSupport with DefaultJsonProtocol {
  implicit def problemErrorFormat: RootJsonFormat[DependencyProblemError] = jsonFormat2(DependencyProblemError)
  implicit def problemFormat: RootJsonFormat[DependencyProblem]           = jsonFormat5(DependencyProblem)

  implicit class TenantWrapper(private val t: DependencyTenant) extends AnyVal {
    def toApi: Tenant = Tenant(id = t.id)
  }

  implicit class ProblemObjectWrapper(private val t: DependencyProblem.type) extends AnyVal {
    def fromString(body: String): Try[Problem] =
      Try(body.parseJson.convertTo[DependencyProblem]).map(problem =>
        Problem(
          `type` = problem.`type`,
          status = problem.status,
          title = problem.title,
          detail = problem.detail,
          errors = problem.errors.map(_.toApi)
        )
      )
  }
  implicit class ProblemWrapper(private val t: DependencyProblem)            extends AnyVal {
    def toApi: Problem =
      Problem(`type` = t.`type`, status = t.status, title = t.title, detail = t.detail, errors = t.errors.map(_.toApi))
  }

  implicit class ProblemErrorWrapper(private val t: DependencyProblemError) extends AnyVal {
    def toApi: ProblemError = ProblemError(code = t.code, detail = t.detail)
  }

}
