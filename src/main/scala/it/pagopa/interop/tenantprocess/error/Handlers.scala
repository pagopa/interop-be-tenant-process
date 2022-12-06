package it.pagopa.interop.tenantprocess.error

import akka.http.scaladsl.server.StandardRoute
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.utils.errors.{AkkaResponses, ServiceCode}
import it.pagopa.interop.tenantprocess.error.TenantProcessErrors.{
  AttributeAlreadyRevoked,
  AttributeRevocationNotAllowed,
  AttributeVerificationNotAllowed,
  CertifiedAttributeNotFoundInTenant,
  DeclaredAttributeNotFoundInTenant,
  RegistryAttributeNotFound,
  SelfcareIdConflict,
  TenantAttributeNotFound,
  TenantByIdNotFound,
  TenantIsNotACertifier,
  TenantNotFound,
  VerifiedAttributeNotFoundInTenant,
  VerifiedAttributeSelfRevocation,
  VerifiedAttributeSelfVerification
}

import scala.util.{Failure, Try}

object Handlers extends AkkaResponses {
  implicit val serviceCode: ServiceCode = ServiceCode("019")

  def handleProducersRetrieveError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = { case Failure(ex) =>
    internalServerError(ex, logMessage)
  }

  def handleConsumersRetrieveError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = { case Failure(ex) =>
    internalServerError(ex, logMessage)
  }

  def handleTenantUpdateError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = { case Failure(ex) =>
    internalServerError(ex, logMessage)
  }

  def handleInternalTenantUpsertError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: RegistryAttributeNotFound) => badRequest(ex, logMessage)
    case Failure(ex)                            => internalServerError(ex, logMessage)
  }

  def handleM2MTenantUpsertError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: RegistryAttributeNotFound) => badRequest(ex, logMessage)
    case Failure(ex: TenantIsNotACertifier)     => forbidden(ex, logMessage)
    case Failure(ex)                            => internalServerError(ex, logMessage)
  }

  def handleM2MTenantRevokeError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: CertifiedAttributeNotFoundInTenant) => badRequest(ex, logMessage)
    case Failure(ex: TenantIsNotACertifier)              => forbidden(ex, logMessage)
    case Failure(ex: TenantNotFound)                     => notFound(ex, logMessage)
    case Failure(ex: RegistryAttributeNotFound)          => notFound(ex, logMessage)
    case Failure(ex)                                     => internalServerError(ex, logMessage)
  }

  def handleSelfcareTenantUpsertError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: SelfcareIdConflict) => conflict(ex, logMessage)
    case Failure(ex)                     => internalServerError(ex, logMessage)
  }

  def handleDeclaredAttributeAdditionError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: SelfcareIdConflict) => conflict(ex, logMessage)
    case Failure(ex)                     => internalServerError(ex, logMessage)
  }

  def handleDeclaredAttributeRevokeError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: DeclaredAttributeNotFoundInTenant) => badRequest(ex, logMessage)
    case Failure(ex: TenantAttributeNotFound)           => notFound(ex, logMessage)
    case Failure(ex)                                    => internalServerError(ex, logMessage)
  }

  def handleVerifiedAttributeVerificationError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: VerifiedAttributeSelfVerification.type) => forbidden(ex, logMessage)
    case Failure(ex: AttributeVerificationNotAllowed)        => forbidden(ex, logMessage)
    case Failure(ex: TenantByIdNotFound)                     => notFound(ex, logMessage)
    case Failure(ex)                                         => internalServerError(ex, logMessage)
  }

  def handleVerifiedAttributeRevokeError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: VerifiedAttributeNotFoundInTenant)    => badRequest(ex, logMessage)
    case Failure(ex: AttributeAlreadyRevoked)              => badRequest(ex, logMessage)
    case Failure(ex: VerifiedAttributeSelfRevocation.type) => forbidden(ex, logMessage)
    case Failure(ex: AttributeRevocationNotAllowed)        => forbidden(ex, logMessage)
    case Failure(ex: TenantByIdNotFound)                   => notFound(ex, logMessage)
    case Failure(ex)                                       => internalServerError(ex, logMessage)
  }

  def handleTenantRetrieveError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: TenantByIdNotFound) => notFound(ex, logMessage)
    case Failure(ex)                     => internalServerError(ex, logMessage)
  }

}
