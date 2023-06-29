package it.pagopa.interop.tenantprocess.common.readmodel

import it.pagopa.interop.catalogmanagement.model.CatalogItem
import it.pagopa.interop.catalogmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import org.mongodb.scala.model.Filters

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object ReadModelCatalogQueries extends ReadModelQuery {

  def getEServiceById(
    eServiceId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[Option[CatalogItem]] = {
    readModel.findOne[CatalogItem](collectionName = "eservices", filter = Filters.eq("data.id", eServiceId.toString))
  }
}
