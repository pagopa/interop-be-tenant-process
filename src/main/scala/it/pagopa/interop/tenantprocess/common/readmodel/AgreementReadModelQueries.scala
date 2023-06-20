package it.pagopa.interop.tenantprocess.common.readmodel

import it.pagopa.interop.agreementmanagement.model.agreement.{PersistentAgreement, PersistentAgreementState}
import it.pagopa.interop.agreementmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object AgreementReadModelQueries extends ReadModelQuery {

  private def agreementsFilters(
    producerId: UUID,
    consumerId: UUID,
    agreementStates: Seq[PersistentAgreementState]
  ): Bson = {
    val producerFilter = Filters.eq("data.producerId", producerId.toString)
    val consumerFilter = Filters.eq("data.consumerId", consumerId.toString)
    val statesFilter   = mapToVarArgs(agreementStates.map(Filters.eq("data.state", _)))(Filters.or)

    mapToVarArgs(Seq(producerFilter, consumerFilter) ++ statesFilter)(Filters.and).getOrElse(Filters.empty())
  }

  def getAgreements(
    producerId: UUID,
    consumerId: UUID,
    agreementStates: Seq[PersistentAgreementState],
    offset: Int,
    limit: Int
  )(readModel: ReadModelService)(implicit ec: ExecutionContext): Future[Seq[PersistentAgreement]] = {
    val filter: Bson = agreementsFilters(producerId, consumerId, agreementStates)
    readModel.find[PersistentAgreement](collectionName = "agreements", filter = filter, offset = offset, limit = limit)
  }
}
