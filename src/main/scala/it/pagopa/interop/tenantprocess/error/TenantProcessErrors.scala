package it.pagopa.interop.tenantprocess.error

import it.pagopa.interop.commons.utils.errors.ComponentError

object TenantProcessErrors {

  object CreateTenantBadRequest extends ComponentError("0001", s"Error creating Tenant")
  final case class GetTenantBadRequest(tenantId: String)
      extends ComponentError("0002", s"Error retrieving Tenant $tenantId")

}
