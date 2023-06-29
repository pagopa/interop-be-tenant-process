package it.pagopa.interop.tenantprocess.common.readmodel

import it.pagopa.interop.agreementmanagement.model.agreement.{PersistentAgreement, PersistentAgreementState}
import it.pagopa.interop.agreementmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object ReadModelAgreementQueries extends ReadModelQuery {

  def getAllAgreements(producerId: UUID, consumerId: UUID, agreementStates: Seq[PersistentAgreementState])(implicit
    ec: ExecutionContext,
    readModel: ReadModelService
  ): Future[Seq[PersistentAgreement]] = {

    def getAgreementsFrom(offset: Int): Future[Seq[PersistentAgreement]] =
      getAgreements(
        producerId = producerId,
        consumerId = consumerId,
        agreementStates = agreementStates,
        limit = 50,
        offset = offset
      )

    def go(start: Int)(as: Seq[PersistentAgreement]): Future[Seq[PersistentAgreement]] =
      getAgreementsFrom(start).flatMap(esec =>
        if (esec.size < 50) Future.successful(as ++ esec) else go(start + 50)(as ++ esec)
      )

    go(0)(Nil)
  }

  private def agreementsFilters(
    producerId: UUID,
    consumerId: UUID,
    agreementStates: Seq[PersistentAgreementState]
  ): Bson = {
    val producerFilter = Filters.eq("data.producerId", producerId.toString)
    val consumerFilter = Filters.eq("data.consumerId", consumerId.toString)
    val statesFilter   = mapToVarArgs(agreementStates.map(a => Filters.eq("data.state", a.toString)))(Filters.or)

    mapToVarArgs(Seq(producerFilter, consumerFilter) ++ statesFilter)(Filters.and).getOrElse(Filters.empty())
  }

  def getAgreements(
    producerId: UUID,
    consumerId: UUID,
    agreementStates: Seq[PersistentAgreementState],
    offset: Int,
    limit: Int
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[Seq[PersistentAgreement]] = {
    val filter: Bson = agreementsFilters(producerId, consumerId, agreementStates)
    readModel.find[PersistentAgreement](collectionName = "agreements", filter = filter, offset = offset, limit = limit)
  }
}
