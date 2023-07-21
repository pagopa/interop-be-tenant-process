package it.pagopa.interop.tenantprocess.common.readmodel

import it.pagopa.interop.agreementmanagement.model.agreement.{Active, Suspended}
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import it.pagopa.interop.tenantmanagement.model.persistence.JsonFormats._
import org.mongodb.scala.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Aggregates.{`match`, count, lookup, project, sort}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Projections.{computed, fields, include}
import org.mongodb.scala.model.Sorts.ascending

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object ReadModelTenantQueries extends ReadModelQuery {

  def getTenantBySelfcareId(
    selfcareId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[Option[PersistentTenant]] =
    readModel.findOne[PersistentTenant](
      collectionName = "tenants",
      filter = Filters.eq("data.selfcareId", selfcareId.toString)
    )

  def getTenantById(
    tenantId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[Option[PersistentTenant]] =
    readModel.findOne[PersistentTenant](collectionName = "tenants", filter = Filters.eq("data.id", tenantId.toString))

  def getTenants(name: Option[String], offset: Int, limit: Int)(implicit
    ec: ExecutionContext,
    readModel: ReadModelService
  ): Future[PaginatedResult[PersistentTenant]] = {
    val query: Bson               = listTenantsFilters(name)
    val filterPipeline: Seq[Bson] = Seq(`match`(query))

    for {
      // Using aggregate to perform case insensitive sorting
      //   N.B.: Required because DocumentDB does not support collation
      tenants <- readModel.aggregate[PersistentTenant](
        "tenants",
        filterPipeline ++
          Seq(
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
        filterPipeline ++
          Seq(count("totalCount"), project(computed("data", Document("""{ "totalCount" : "$totalCount" }""")))),
        offset = 0,
        limit = Int.MaxValue
      )
    } yield PaginatedResult(results = tenants, totalCount = count.headOption.map(_.totalCount).getOrElse(0))
  }

  def getTenantByExternalId(origin: String, value: String)(implicit
    ec: ExecutionContext,
    readModel: ReadModelService
  ): Future[Option[PersistentTenant]] = readModel.findOne[PersistentTenant](
    collectionName = "tenants",
    filter = Filters.and(Filters.eq("data.externalId.origin", origin), Filters.eq("data.externalId.value", value))
  )

  def listProducers(name: Option[String], offset: Int, limit: Int)(implicit
    ec: ExecutionContext,
    readModel: ReadModelService
  ): Future[PaginatedResult[PersistentTenant]] = {
    val query: Bson               = listTenantsFilters(name)
    val filterPipeline: Seq[Bson] = Seq(
      `match`(query),
      lookup("eservices", "data.id", "data.producerId", "eservices"),
      `match`(Filters.not(Filters.size("eservices", 0)))
    )

    for {
      // Using aggregate to perform case insensitive sorting
      //   N.B.: Required because DocumentDB does not support collation
      tenants <- readModel.aggregate[PersistentTenant](
        "tenants",
        filterPipeline ++
          Seq(
            project(fields(include("data"), computed("lowerName", Document("""{ "$toLower" : "$data.name" }""")))),
            sort(ascending("lowerName"))
          ),
        offset = offset,
        limit = limit,
        allowDiskUse = true
      )
      // Note: This could be obtained using $facet function (avoiding to execute the query twice),
      //   but it is not supported by DocumentDB
      count   <- readModel.aggregate[TotalCountResult](
        "tenants",
        filterPipeline ++
          Seq(count("totalCount"), project(computed("data", Document("""{ "totalCount" : "$totalCount" }""")))),
        offset = 0,
        limit = Int.MaxValue,
        allowDiskUse = true
      )
    } yield PaginatedResult(results = tenants, totalCount = count.headOption.map(_.totalCount).getOrElse(0))
  }

  def listConsumers(name: Option[String], producerId: UUID, offset: Int, limit: Int)(implicit
    ec: ExecutionContext,
    readModel: ReadModelService
  ): Future[PaginatedResult[PersistentTenant]] = {
    val query: Bson               = listTenantsFilters(name)
    val filterPipeline: Seq[Bson] = Seq(
      `match`(query),
      lookup("agreements", "data.id", "data.consumerId", "agreements"),
      `match`(
        Filters.and(
          Filters.eq("agreements.data.producerId", producerId.toString),
          Filters.in("agreements.data.state", Active.toString, Suspended.toString)
        )
      )
    )

    for {
      // Using aggregate to perform case insensitive sorting
      //   N.B.: Required because DocumentDB does not support collation
      tenants <- readModel.aggregate[PersistentTenant](
        "tenants",
        filterPipeline ++
          Seq(
            project(fields(include("data"), computed("lowerName", Document("""{ "$toLower" : "$data.name" }""")))),
            sort(ascending("lowerName"))
          ),
        offset = offset,
        limit = limit,
        allowDiskUse = true
      )
      // Note: This could be obtained using $facet function (avoiding to execute the query twice),
      //   but it is not supported by DocumentDB
      count   <- readModel.aggregate[TotalCountResult](
        "tenants",
        filterPipeline ++
          Seq(count("totalCount"), project(computed("data", Document("""{ "totalCount" : "$totalCount" }""")))),
        offset = 0,
        limit = Int.MaxValue,
        allowDiskUse = true
      )
    } yield PaginatedResult(results = tenants, totalCount = count.headOption.map(_.totalCount).getOrElse(0))
  }

  private def listTenantsFilters(name: Option[String]): Bson = {
    val nameFilter           = name match {
      case Some(n) if n.nonEmpty => List(Filters.regex("data.name", n, "i"))
      case _                     => Nil
    }
    val withSelfcareIdFilter = Filters.exists("data.selfcareId", true)

    val filters = withSelfcareIdFilter :: nameFilter
    mapToVarArgs(filters)(Filters.and).getOrElse(Filters.empty())
  }
}
