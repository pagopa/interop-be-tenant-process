package it.pagopa.interop.tenantprocess.common.readmodel

import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import it.pagopa.interop.tenantmanagement.model.persistence.JsonFormats._
import org.mongodb.scala.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Aggregates.{`match`, count, lookup, project, sort}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Projections.{computed, fields, include}
import org.mongodb.scala.model.Sorts.ascending

import scala.concurrent.{ExecutionContext, Future}

object ReadModelQueries {

  def listProducers(name: Option[String], offset: Int, limit: Int)(
    readModel: ReadModelService
  )(implicit ec: ExecutionContext): Future[PaginatedResult[PersistentTenant]] = {
    val query = listTenantsFilters(name)

    for {
      // Using aggregate to perform case insensitive sorting
      //   N.B.: Required because DocumentDB does not support collation
      tenants <- readModel.aggregate[PersistentTenant](
        "tenants",
        Seq(
          `match`(query),
          lookup("eservices", "data.id", "data.producerId", "eservices"),
          `match`(Filters.not(Filters.size("eservices", 0))),
          project(fields(include("data"), computed("lowerName", Document("""{ "$toLower" : "$data.name" }""")))),
          sort(ascending("lowerName"))
        ),
        offset = offset,
        limit = limit
      )
      // Note: This could be obtained using $facet function (avoiding to execute the query twice),
      //   but it is not supported by DocumentDB
      count   <- readModel.aggregate[TotalCountResult](
        "tenants",
        Seq(
          `match`(query),
          lookup("eservices", "data.id", "data.producerId", "eservices"),
          `match`(Filters.not(Filters.size("eservices", 0))),
          count("totalCount"),
          project(computed("data", Document("""{ "totalCount" : "$totalCount" }""")))
        ),
        offset = 0,
        limit = Int.MaxValue
      )
    } yield PaginatedResult(results = tenants, totalCount = count.headOption.map(_.totalCount).getOrElse(0))
  }

  def listConsumers(name: Option[String], offset: Int, limit: Int)(
    readModel: ReadModelService
  )(implicit ec: ExecutionContext): Future[PaginatedResult[PersistentTenant]] = {
    val query = listTenantsFilters(name)

    for {
      // Using aggregate to perform case insensitive sorting
      //   N.B.: Required because DocumentDB does not support collation
      tenants <- readModel.aggregate[PersistentTenant](
        "tenants",
        Seq(
          `match`(query),
          lookup("agreements", "data.id", "data.consumerId", "agreements"),
          `match`(Filters.not(Filters.size("agreements", 0))),
          project(fields(include("data"), computed("lowerName", Document("""{ "$toLower" : "$data.name" }""")))),
          sort(ascending("lowerName"))
        ),
        offset = offset,
        limit = limit
      )
      // Note: This could be obtained using $facet function (avoiding to execute the query twice),
      //   but it is not supported by DocumentDB
      count   <- readModel.aggregate[TotalCountResult](
        "tenants",
        Seq(
          `match`(query),
          lookup("agreements", "data.id", "data.consumerId", "agreements"),
          `match`(Filters.not(Filters.size("agreements", 0))),
          count("totalCount"),
          project(computed("data", Document("""{ "totalCount" : "$totalCount" }""")))
        ),
        offset = 0,
        limit = Int.MaxValue
      )
    } yield PaginatedResult(results = tenants, totalCount = count.headOption.map(_.totalCount).getOrElse(0))
  }

  def listTenantsFilters(name: Option[String]): Bson = {
    val nameFilter = name.map(Filters.regex("data.name", _, "i"))

    mapToVarArgs(nameFilter.toList)(Filters.and).getOrElse(Filters.empty())
  }

  def mapToVarArgs[A, B](l: Seq[A])(f: Seq[A] => B): Option[B] = Option.when(l.nonEmpty)(f(l))
}
