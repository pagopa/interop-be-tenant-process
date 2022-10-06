package it.pagopa.interop.tenantprocess.service

import it.pagopa.interop.agreementmanagement.client.model.{Agreement, AgreementState}

import java.util.UUID
import scala.concurrent.Future

trait AgreementManagementService {
  def getAgreements(producerId: UUID, consumerId: UUID, states: Seq[AgreementState])(implicit
    contexts: Seq[(String, String)]
  ): Future[Seq[Agreement]]
}
