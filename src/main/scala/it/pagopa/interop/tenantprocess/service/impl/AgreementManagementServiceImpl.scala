package it.pagopa.interop.tenantprocess.service.impl

import it.pagopa.interop.tenantprocess.service.AgreementManagementService
import it.pagopa.interop.agreementmanagement.model.agreement.{PersistentAgreement, PersistentAgreementState}
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.tenantprocess.common.readmodel.ReadModelAgreementQueries

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}

final object AgreementManagementServiceImpl extends AgreementManagementService {
  override def getAgreements(producerId: UUID, consumerId: UUID, states: Seq[PersistentAgreementState])(implicit
    ec: ExecutionContext,
    readModel: ReadModelService
  ): Future[Seq[PersistentAgreement]] =
    ReadModelAgreementQueries.getAllAgreements(producerId, consumerId, states)
}
