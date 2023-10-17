package it.pagopa.interop.tenantprocess.error

import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.OperationForbidden
import it.pagopa.interop.commons.utils.errors.{AkkaResponses, ServiceCode}
import it.pagopa.interop.tenantprocess.error.TenantProcessErrors._

import scala.util.{Failure, Success, Try}

object ResponseHandlers extends AkkaResponses {
  implicit val serviceCode: ServiceCode = ServiceCode("019")

  def getProducersResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)  => success(s)
      case Failure(ex) => internalServerError(ex, logMessage)
    }

  def getConsumersResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)  => success(s)
      case Failure(ex) => internalServerError(ex, logMessage)
    }

  def getTenantsResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)  => success(s)
      case Failure(ex) => internalServerError(ex, logMessage)
    }

  def updateTenantResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                           => success(s)
      case Failure(ex: OperationForbidden.type) => forbidden(ex, logMessage)
      case Failure(ex: TenantByIdNotFound)      => notFound(ex, logMessage)
      case Failure(ex)                          => internalServerError(ex, logMessage)
    }

  def internalUpsertTenantResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                             => success(s)
      case Failure(ex: RegistryAttributeNotFound) => badRequest(ex, logMessage)
      case Failure(ex)                            => internalServerError(ex, logMessage)
    }

  def internalRevokeCertifiedAttributeResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                      => success(s)
      case Failure(ex: CertifiedAttributeNotFoundInTenant) => badRequest(ex, logMessage)
      case Failure(ex: TenantNotFound)                     => notFound(ex, logMessage)
      case Failure(ex: RegistryAttributeNotFound)          => notFound(ex, logMessage)
      case Failure(ex)                                     => internalServerError(ex, logMessage)
    }

  def m2mUpsertTenantResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                             => success(s)
      case Failure(ex: RegistryAttributeNotFound) => badRequest(ex, logMessage)
      case Failure(ex: TenantIsNotACertifier)     => forbidden(ex, logMessage)
      case Failure(ex)                            => internalServerError(ex, logMessage)
    }

  def m2mRevokeAttributeResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                      => success(s)
      case Failure(ex: CertifiedAttributeNotFoundInTenant) => badRequest(ex, logMessage)
      case Failure(ex: TenantIsNotACertifier)              => forbidden(ex, logMessage)
      case Failure(ex: TenantNotFound)                     => notFound(ex, logMessage)
      case Failure(ex: TenantByIdNotFound)                 => notFound(ex, logMessage)
      case Failure(ex: RegistryAttributeNotFound)          => notFound(ex, logMessage)
      case Failure(ex)                                     => internalServerError(ex, logMessage)
    }

  def selfcareUpsertTenantResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                           => success(s)
      case Failure(ex: OperationForbidden.type) => forbidden(ex, logMessage)
      case Failure(ex: SelfcareIdConflict)      => conflict(ex, logMessage)
      case Failure(ex)                          => internalServerError(ex, logMessage)
    }

  def addDeclaredAttributeResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                      => success(s)
      case Failure(ex: SelfcareIdConflict) => conflict(ex, logMessage)
      case Failure(ex)                     => internalServerError(ex, logMessage)
    }

  def revokeDeclaredAttributeResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                     => success(s)
      case Failure(ex: DeclaredAttributeNotFoundInTenant) => badRequest(ex, logMessage)
      case Failure(ex: TenantAttributeNotFound)           => notFound(ex, logMessage)
      case Failure(ex)                                    => internalServerError(ex, logMessage)
    }

  def verifyVerifiedAttributeResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                          => success(s)
      case Failure(ex: VerifiedAttributeSelfVerification.type) => forbidden(ex, logMessage)
      case Failure(ex: AttributeVerificationNotAllowed)        => forbidden(ex, logMessage)
      case Failure(ex: TenantByIdNotFound)                     => notFound(ex, logMessage)
      case Failure(ex)                                         => internalServerError(ex, logMessage)
    }

  def revokeVerifiedAttributeResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                        => success(s)
      case Failure(ex: VerifiedAttributeNotFoundInTenant)    => badRequest(ex, logMessage)
      case Failure(ex: VerifiedAttributeSelfRevocation.type) => forbidden(ex, logMessage)
      case Failure(ex: AttributeRevocationNotAllowed)        => forbidden(ex, logMessage)
      case Failure(ex: TenantByIdNotFound)                   => notFound(ex, logMessage)
      case Failure(ex: AttributeAlreadyRevoked)              => conflict(ex, logMessage)
      case Failure(ex)                                       => internalServerError(ex, logMessage)
    }

  def updateVerifiedAttributeResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                     => success(s)
      case Failure(ex: ExpirationDateCannotBeInThePast)   => badRequest(ex, logMessage)
      case Failure(ex: OrganizationNotFoundInVerifiers)   => forbidden(ex, logMessage)
      case Failure(ex: VerifiedAttributeNotFoundInTenant) => notFound(ex, logMessage)
      case Failure(ex: TenantByIdNotFound)                => notFound(ex, logMessage)
      case Failure(ex)                                    => internalServerError(ex, logMessage)
    }

  def updateVerifiedAttributeExtensionDateResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                     => success(s)
      case Failure(ex: OrganizationNotFoundInVerifiers)   => forbidden(ex, logMessage)
      case Failure(ex: VerifiedAttributeNotFoundInTenant) => notFound(ex, logMessage)
      case Failure(ex: ExpirationDateNotFoundInVerifier)  => badRequest(ex, logMessage)
      case Failure(ex: TenantByIdNotFound)                => notFound(ex, logMessage)
      case Failure(ex)                                    => internalServerError(ex, logMessage)
    }

  def getTenantBySelfcareIdResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                     => success(s)
      case Failure(ex: SelcareIdNotFound) => notFound(ex, logMessage)
      case Failure(ex)                    => internalServerError(ex, logMessage)
    }

  def getTenantResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                      => success(s)
      case Failure(ex: TenantByIdNotFound) => notFound(ex, logMessage)
      case Failure(ex)                     => internalServerError(ex, logMessage)
    }

  def getTenantByExternalIdResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                  => success(s)
      case Failure(ex: TenantNotFound) => notFound(ex, logMessage)
      case Failure(ex)                 => internalServerError(ex, logMessage)
    }

  def internalAssignCertifiedAttributeResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                     => success(s)
      case Failure(ex: CertifiedAttributeAlreadyInTenant) => conflict(ex, logMessage)
      case Failure(ex: TenantNotFound)                    => notFound(ex, logMessage)
      case Failure(ex: RegistryAttributeNotFound)         => notFound(ex, logMessage)
      case Failure(ex)                                    => internalServerError(ex, logMessage)
    }
}
