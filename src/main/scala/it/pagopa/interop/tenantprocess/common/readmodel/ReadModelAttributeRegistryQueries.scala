package it.pagopa.interop.tenantprocess.common.readmodel

import it.pagopa.interop.attributeregistrymanagement.model.persistence.attribute.PersistentAttribute
import it.pagopa.interop.attributeregistrymanagement.model.persistence.JsonFormats._
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import org.mongodb.scala.model.Filters

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object ReadModelAttributeRegistryQueries extends ReadModelQuery {
  def getAttributeByExternalCode(origin: String, code: String)(implicit
    ec: ExecutionContext,
    readModel: ReadModelService
  ): Future[Option[PersistentAttribute]] = {
    readModel.findOne[PersistentAttribute](
      collectionName = "attributes",
      filter = Filters.and(Filters.eq("data.origin", origin), Filters.eq("data.code", code))
    )
  }
  def getAttributeById(
    attributeId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[Option[PersistentAttribute]] = {
    readModel
      .findOne[PersistentAttribute](collectionName = "attributes", filter = Filters.eq("data.id", attributeId.toString))
  }
}
