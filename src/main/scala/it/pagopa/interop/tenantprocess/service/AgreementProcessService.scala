package it.pagopa.interop.tenantprocess.service

import java.util.UUID
import scala.concurrent.Future

trait AgreementProcessService {
  def computeAgreementsByAttribute(consumerId: UUID, attributeId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit]
}
