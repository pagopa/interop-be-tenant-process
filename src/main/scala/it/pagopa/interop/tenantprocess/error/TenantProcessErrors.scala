package it.pagopa.interop.tenantprocess.error

import it.pagopa.interop.commons.utils.errors.ComponentError

object TenantProcessErrors {
  final case class TenantIsNotACertifier(tenantId: String)
      extends ComponentError("0001", s"Organization $tenantId not allowed")

}
