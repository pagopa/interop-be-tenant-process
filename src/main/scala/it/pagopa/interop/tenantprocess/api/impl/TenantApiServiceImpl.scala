package it.pagopa.interop.tenantprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.{Route, StandardRoute}
import cats.implicits._
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.agreementmanagement.client.model.AgreementState
import it.pagopa.interop.attributeregistrymanagement.client.invoker.{ApiError => AttributeRegistryApiError}
import it.pagopa.interop.attributeregistrymanagement.client.model.Attribute
import it.pagopa.interop.commons.jwt._
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils.getOrganizationIdFutureUUID
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.{GenericError, OperationForbidden}
import it.pagopa.interop.commons.utils.service.{OffsetDateTimeSupplier, UUIDSupplier}
import it.pagopa.interop.tenantmanagement.client
import it.pagopa.interop.tenantmanagement.client.invoker.{ApiError => TenantApiError}
import it.pagopa.interop.tenantmanagement.client.model.{
  TenantDelta,
  TenantFeature,
  Certifier => DependencyCertifier,
  Problem => TenantProblem,
  Tenant => DependencyTenant,
  VerifiedTenantAttribute => DependencyVerifiedTenantAttribute,
  TenantVerifier => DependencyTenantVerifier,
  TenantAttribute => DependencyTenantAttribute,
  TenantRevoker => DependencyTenantRevoker
}
import it.pagopa.interop.tenantprocess.api.TenantApiService
import it.pagopa.interop.tenantprocess.api.adapters.AdaptableSeed
import it.pagopa.interop.tenantprocess.api.adapters.AdaptableSeed._
import it.pagopa.interop.tenantprocess.api.adapters.ApiAdapters._
import it.pagopa.interop.tenantprocess.api.adapters.AttributeRegistryManagementAdapters._
import it.pagopa.interop.tenantprocess.api.adapters.TenantManagementAdapters._
import it.pagopa.interop.tenantprocess.error.TenantProcessErrors._
import it.pagopa.interop.tenantprocess.model._
import it.pagopa.interop.tenantprocess.service._

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

final case class TenantApiServiceImpl(
  attributeRegistryManagementService: AttributeRegistryManagementService,
  tenantManagementService: TenantManagementService,
  agreementProcessService: AgreementProcessService,
  agreementManagementService: AgreementManagementService,
  catalogManagementService: CatalogManagementService,
  uuidSupplier: UUIDSupplier,
  dateTimeSupplier: OffsetDateTimeSupplier
)(implicit ec: ExecutionContext)
    extends TenantApiService {

  private val logger = Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  private[this] def authorize(roles: String*)(
    route: => Route
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    authorizeInterop(hasPermissions(roles: _*), problemOf(StatusCodes.Forbidden, OperationForbidden)) {
      route
    }

  override def internalUpsertTenant(seed: InternalTenantSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = authorize(INTERNAL_ROLE) {
    logger.info(s"Creating tenant with external id ${seed.externalId} via internal request")

    val now: OffsetDateTime = dateTimeSupplier.get()

    val result: Future[Tenant] = for {
      existingTenant <- findTenant(seed.externalId)
      attributesIds = seed.certifiedAttributes.map(a => ExternalId(a.origin, a.code))
      tenant <- existingTenant.fold(createTenant(seed, attributesIds, now))(
        updateTenantCertifiedAttributes(attributesIds, now)
      )
    } yield tenant.toApi

    onComplete(result) {
      handleApiError() orElse {
        case Success(tenant) =>
          internalUpsertTenant200(tenant)
        case Failure(ex)     =>
          logger.error(s"Error creating tenant with external id ${seed.externalId} via internal request", ex)
          internalServerError()
      }
    }
  }

  override def m2mUpsertTenant(seed: M2MTenantSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route =
    authorize(M2M_ROLE) {
      logger.info(s"Creating tenant with external id ${seed.externalId} via m2m request")

      val now: OffsetDateTime = dateTimeSupplier.get()

      def validateCertifierTenant: Future[DependencyCertifier] = for {
        requesterTenantUuid <- getOrganizationIdFutureUUID(contexts)
        requesterTenant     <- tenantManagementService.getTenant(requesterTenantUuid)
        maybeCertifier = requesterTenant.features.collectFirst { case TenantFeature(Some(certifier)) => certifier }
        certifier <- maybeCertifier.toFuture(TenantIsNotACertifier(requesterTenantUuid))
      } yield certifier

      val result: Future[Tenant] = for {
        certifier      <- validateCertifierTenant
        existingTenant <- findTenant(seed.externalId)
        attributesId = seed.certifiedAttributes.map(a => ExternalId(certifier.certifierId, a.code))
        tenant <- existingTenant.fold(createTenant(seed, attributesId, now))(
          updateTenantCertifiedAttributes(attributesId, now)
        )
      } yield tenant.toApi

      onComplete(result) {
        handleApiError() orElse {
          case Success(tenant)                    =>
            m2mUpsertTenant200(tenant)
          case Failure(ex: TenantIsNotACertifier) =>
            logger.error(s"Error creating tenant with external id ${seed.externalId} via m2m request", ex)
            m2mUpsertTenant403(problemOf(StatusCodes.Forbidden, ex))
          case Failure(ex)                        =>
            logger.error(s"Error creating tenant with external id ${seed.externalId} via m2m request", ex)
            internalServerError()
        }
      }
    }

  override def m2mRevokeAttribute(origin: String, externalId: String, code: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(M2M_ROLE) {
    logger.info(s"Revoking attribute $code from tenant ($origin,$externalId) via m2m request")

    val result: Future[Unit] = for {
      requesterTenantUuid <- getOrganizationIdFutureUUID(contexts)
      requesterTenant     <- tenantManagementService.getTenant(requesterTenantUuid)
      certifierId         <- requesterTenant.features
        .collectFirstSome(_.certifier.map(_.certifierId))
        .toFuture(TenantIsNotACertifier(requesterTenantUuid))
      tenantToModify      <- tenantManagementService.getTenantByExternalId(client.model.ExternalId(origin, externalId))
      attributeIdToRevoke <- attributeRegistryManagementService
        .getAttributeByExternalCode(certifierId, code)
        .map(_.id)
        .recoverWith {
          case x: AttributeRegistryApiError[_] if x.code < 500 =>
            Future.failed(CertifiedAttributeNotFound(origin, certifierId))
        }
      attributeToModify   <- tenantToModify.attributes
        .mapFilter(_.certified)
        .find(_.id == attributeIdToRevoke)
        .toFuture(CertifiedAttributeNotFound(origin, certifierId))
      modifiedAttribute = attributeToModify.copy(revocationTimestamp = dateTimeSupplier.get().some)
      () <- tenantManagementService
        .updateTenantAttribute(
          tenantToModify.id,
          attributeToModify.id,
          client.model.TenantAttribute(certified = modifiedAttribute.some)
        )
        .void
      _  <- agreementProcessService.computeAgreementsByAttribute(tenantToModify.id, attributeIdToRevoke)
    } yield ()

    onComplete(result) {
      handleApiError() orElse {
        case Success(())                             => m2mRevokeAttribute204
        case Failure(ex: TenantIsNotACertifier)      =>
          logger.error(s"Error revoking attribute $code from tenant ($origin,$externalId) via m2m request", ex)
          m2mRevokeAttribute403(problemOf(StatusCodes.Forbidden, ex))
        case Failure(ex: CertifiedAttributeNotFound) =>
          logger.error(s"Error revoking attribute $code from tenant ($origin,$externalId) via m2m request", ex)
          m2mRevokeAttribute400(problemOf(StatusCodes.BadRequest, ex))
        case Failure(ex)                             =>
          logger.error(s"Error revoking attribute $code from tenant ($origin,$externalId) via m2m request", ex)
          internalServerError()
      }
    }
  }

  override def selfcareUpsertTenant(seed: SelfcareTenantSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = authorize(ADMIN_ROLE, API_ROLE, SECURITY_ROLE, INTERNAL_ROLE) {
    logger.info(s"Creating tenant with external id ${seed.externalId} via SelfCare request")

    val now: OffsetDateTime = dateTimeSupplier.get()

    def updateSelfcareId(tenant: DependencyTenant): Future[DependencyTenant] = {
      def updateTenant(): Future[DependencyTenant]                     = tenantManagementService
        .updateTenant(tenant.id, TenantDelta(selfcareId = seed.selfcareId.some, features = tenant.features))
      def verifyConflict(selfcareId: String): Future[DependencyTenant] = Future
        .failed(SelfcareIdConflict(tenant.id, selfcareId, seed.selfcareId))
        .whenA(selfcareId != seed.selfcareId)
        .as(tenant)

      tenant.selfcareId.fold(updateTenant())(verifyConflict)
    }

    val result: Future[Tenant] = for {
      existingTenant <- findTenant(seed.externalId)
      tenant         <- existingTenant.fold(createTenant(seed, Nil, now))(Future.successful)
      updatedTenant  <- updateSelfcareId(tenant)
    } yield updatedTenant.toApi

    onComplete(result) {
      handleApiError() orElse {
        case Success(tenant)                 => selfcareUpsertTenant200(tenant)
        case Failure(ex: SelfcareIdConflict) =>
          logger.error(s"Error creating tenant with external id ${seed.externalId} via SelfCare request", ex)
          selfcareUpsertTenant409(problemOf(StatusCodes.Conflict, ex))
        case Failure(ex)                     =>
          logger.error(s"Error creating tenant with external id ${seed.externalId} via SelfCare request", ex)
          internalServerError()
      }
    }
  }

  override def addDeclaredAttribute(seed: DeclaredTenantAttributeSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = authorize(ADMIN_ROLE) {
    logger.info(s"Adding declared attribute ${seed.id} to requester tenant")

    val now: OffsetDateTime = dateTimeSupplier.get()

    def addAttribute(tenantId: UUID, seed: DeclaredTenantAttributeSeed): Future[DependencyTenant] =
      tenantManagementService.addTenantAttribute(tenantId, seed.toDependency(now))

    def updateAttribute(tenantId: UUID, attribute: DependencyTenantAttribute): Future[DependencyTenant] = for {
      declaredAttribute <- attribute.declared.toFuture(new RuntimeException())
      updateSeed = attribute.copy(declared = declaredAttribute.copy(revocationTimestamp = None).some)
      updatedAttribute <- tenantManagementService.updateTenantAttribute(tenantId, declaredAttribute.id, updateSeed)
    } yield updatedAttribute

    def upsertAttribute(tenantId: UUID, seed: DeclaredTenantAttributeSeed): Future[DependencyTenant] = for {
      maybeAttribute <- tenantManagementService
        .getTenantAttribute(tenantId, seed.id)
        .map(Some(_))
        .recover {
          case err: TenantApiError[_] if err.code == 404 => None
        }
      updatedTenant  <- maybeAttribute.fold(addAttribute(tenantId, seed))(updateAttribute(tenantId, _))
    } yield updatedTenant

    val result: Future[Tenant] = for {
      requesterTenantUuid <- getOrganizationIdFutureUUID(contexts)
      _ = logger.info(s"Adding declared attribute ${seed.id} to $requesterTenantUuid")
      tenant <- upsertAttribute(requesterTenantUuid, seed)
      _      <- agreementProcessService.computeAgreementsByAttribute(requesterTenantUuid, seed.id)
    } yield tenant.toApi

    onComplete(result) {
      handleApiError() orElse {
        case Success(tenant) => addDeclaredAttribute200(tenant)
        case Failure(ex)     =>
          logger.error(s"Error adding declared attribute ${seed.id} to requester tenant", ex)
          internalServerError()
      }
    }
  }

  override def revokeDeclaredAttribute(attributeId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = authorize(ADMIN_ROLE) {
    logger.info(s"Revoking declared attribute $attributeId to requester tenant")

    val now: OffsetDateTime = dateTimeSupplier.get()

    val result: Future[Tenant] = for {
      requesterTenantUuid <- getOrganizationIdFutureUUID(contexts)
      _ = logger.info(s"Revoking declared attribute $attributeId to $requesterTenantUuid")
      attributeUuid     <- attributeId.toFutureUUID
      attribute         <- tenantManagementService.getTenantAttribute(requesterTenantUuid, attributeUuid)
      declaredAttribute <- attribute.declared.toFuture(DeclaredAttributeNotFound(requesterTenantUuid, attributeId))
      revokedAttribute = declaredAttribute.copy(revocationTimestamp = now.some).toTenantAttribute
      tenant <- tenantManagementService.updateTenantAttribute(requesterTenantUuid, attributeUuid, revokedAttribute)
      _      <- agreementProcessService.computeAgreementsByAttribute(requesterTenantUuid, attributeUuid)
    } yield tenant.toApi

    onComplete(result) {
      handleApiError() orElse {
        case Success(tenant)                        => revokeDeclaredAttribute200(tenant)
        case Failure(ex: DeclaredAttributeNotFound) =>
          logger.error(s"Error revoking declared attribute $attributeId to requester tenant", ex)
          revokeDeclaredAttribute404(problemOf(StatusCodes.NotFound, ex))
        case Failure(ex)                            =>
          logger.error(s"Error revoking declared attribute $attributeId to requester tenant", ex)
          internalServerError()
      }
    }
  }

  override def verifyVerifiedAttribute(tenantId: String, seed: VerifiedTenantAttributeSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = authorize(ADMIN_ROLE) {
    logger.info(s"Verifying attribute ${seed.id} to tenant $tenantId")

    val now: OffsetDateTime = dateTimeSupplier.get()

    val result: Future[Tenant] = for {
      requesterTenantUuid <- getOrganizationIdFutureUUID(contexts)
      targetTenantUuid    <- tenantId.toFutureUUID
      _            <- Future.failed(VerifiedAttributeSelfVerification).whenA(requesterTenantUuid == targetTenantUuid)
      _            <- assertAttributeVerificationAllowed(requesterTenantUuid, targetTenantUuid, seed.id)
      targetTenant <- tenantManagementService.getTenant(targetTenantUuid)
      attribute = targetTenant.attributes.flatMap(_.verified).find(_.id == seed.id)
      updatedTenant <- attribute.fold(
        tenantManagementService.addTenantAttribute(targetTenantUuid, seed.toCreateDependency(now, requesterTenantUuid))
      )(attr =>
        tenantManagementService.updateTenantAttribute(
          targetTenantUuid,
          seed.id,
          seed.toUpdateDependency(now, requesterTenantUuid, attr)
        )
      )
      _             <- agreementProcessService.computeAgreementsByAttribute(targetTenantUuid, seed.id)
    } yield updatedTenant.toApi

    onComplete(result) {
      handleApiError() orElse {
        case Success(tenant)                                     => verifyVerifiedAttribute200(tenant)
        case Failure(ex: AttributeAlreadyVerified)               =>
          logger.error(s"Error verifying attribute ${seed.id} to tenant $tenantId", ex)
          verifyVerifiedAttribute409(problemOf(StatusCodes.Conflict, ex))
        case Failure(ex: VerifiedAttributeSelfVerification.type) =>
          logger.error(s"Error verifying attribute ${seed.id} to tenant $tenantId", ex)
          verifyVerifiedAttribute403(problemOf(StatusCodes.Forbidden, ex))
        case Failure(ex: AttributeVerificationNotAllowed)        =>
          logger.error(s"Error verifying attribute ${seed.id} to tenant $tenantId", ex)
          verifyVerifiedAttribute403(problemOf(StatusCodes.Forbidden, ex))
        case Failure(ex)                                         =>
          logger.error(s"Error verifying attribute ${seed.id} to tenant $tenantId", ex)
          internalServerError()
      }
    }
  }

  override def revokeVerifiedAttribute(tenantId: String, attributeId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = authorize(ADMIN_ROLE) {
    logger.info(s"Revoking attribute $attributeId to tenant $tenantId")

    val now: OffsetDateTime = dateTimeSupplier.get()

    val result: Future[Tenant] = for {
      requesterTenantUuid <- getOrganizationIdFutureUUID(contexts)
      targetTenantUuid    <- tenantId.toFutureUUID
      attributeUuid       <- attributeId.toFutureUUID
      _             <- Future.failed(VerifiedAttributeSelfRevocation).whenA(requesterTenantUuid == targetTenantUuid)
      _             <- assertAttributeRevocationAllowed(requesterTenantUuid, targetTenantUuid, attributeUuid)
      targetTenant  <- tenantManagementService.getTenant(targetTenantUuid)
      attribute     <- targetTenant.attributes
        .flatMap(_.verified)
        .find(_.id == attributeUuid)
        .toFuture(VerifiedAttributeNotFound(targetTenantUuid, attributeId))
      // TODO Not sure if this is compatible with implicit verification
      verifier      <- attribute.verifiedBy
        .find(_.id == requesterTenantUuid)
        .toFuture(AttributeRevocationNotAllowed(targetTenantUuid, attributeUuid))
      _             <- Future
        .failed(AttributeAlreadyRevoked(targetTenantUuid, requesterTenantUuid, attributeUuid))
        .unlessA(attribute.verifiedBy.exists(_.id == requesterTenantUuid))
        .whenA(attribute.revokedBy.exists(_.id == requesterTenantUuid))
      updatedTenant <- tenantManagementService.updateTenantAttribute(
        targetTenantUuid,
        attributeUuid,
        addRevoker(attribute, now, verifier).toTenantAttribute
      )
      _             <- agreementProcessService.computeAgreementsByAttribute(targetTenantUuid, attributeUuid)
    } yield updatedTenant.toApi

    onComplete(result) {
      handleApiError() orElse {
        case Success(tenant)                                   => revokeVerifiedAttribute200(tenant)
        case Failure(ex: VerifiedAttributeSelfRevocation.type) =>
          logger.error(s"Error revoking attribute $attributeId to tenant $tenantId", ex)
          revokeVerifiedAttribute403(problemOf(StatusCodes.Forbidden, ex))
        case Failure(ex: AttributeRevocationNotAllowed)        =>
          logger.error(s"Error revoking attribute $attributeId to tenant $tenantId", ex)
          revokeVerifiedAttribute403(problemOf(StatusCodes.Forbidden, ex))
        case Failure(ex: VerifiedAttributeNotFound)            =>
          logger.error(s"Error revoking attribute $attributeId to tenant $tenantId", ex)
          revokeVerifiedAttribute404(problemOf(StatusCodes.NotFound, ex))
        case Failure(ex: AttributeAlreadyRevoked)              =>
          logger.error(s"Error revoking attribute $attributeId to tenant $tenantId", ex)
          revokeVerifiedAttribute409(problemOf(StatusCodes.Conflict, ex))
        case Failure(ex)                                       =>
          logger.error(s"Error revoking attribute $attributeId to tenant $tenantId", ex)
          internalServerError()
      }
    }
  }

  private def createTenant[T: AdaptableSeed](seed: T, attributes: Seq[ExternalId], timestamp: OffsetDateTime)(implicit
    contexts: Seq[(String, String)]
  ): Future[DependencyTenant] =
    for {
      attributes <- getAttributes(attributes)
      dependencyAttributes = attributes.map(_.toCertifiedSeed(timestamp))
      tenantId             = uuidSupplier.get()
      tenant <- tenantManagementService.createTenant(toDependency(seed, tenantId, dependencyAttributes))
    } yield tenant

  private def updateTenantCertifiedAttributes(attributes: Seq[ExternalId], timestamp: OffsetDateTime)(
    tenant: DependencyTenant
  )(implicit contexts: Seq[(String, String)]): Future[DependencyTenant] = {
    def computeAgreements(attributesIds: Seq[UUID]): Future[Seq[Unit]] =
      Future.traverse(attributesIds)(agreementProcessService.computeAgreementsByAttribute(tenant.id, _))

    for {
      attributes <- getAttributes(attributes)
      // TODO tenant.attributes can be an issue in case of pagination. Create a tenantManagementService.getAttribute?
      (existingAttributes, newAttributes) = attributes.partition(attr =>
        tenant.attributes.mapFilter(_.certified).exists(_.id == attr.id)
      )
      () <- Future
        .traverse(newAttributes)(a =>
          tenantManagementService.addTenantAttribute(tenant.id, a.toCertifiedSeed(timestamp))
        )
        .void
      existingAttributesIds      = existingAttributes.map(_.id)
      reactivateTenantAttributes = tenant.attributes
        // Note: the filter considers revocationTimestamp the only field that can change.
        //       If more fields would be updated in the future, this filter must be revisited
        .mapFilter(_.certified.filter(a => existingAttributesIds.contains(a.id) && a.revocationTimestamp.nonEmpty))
        .map(c => (c.id, client.model.TenantAttribute(certified = c.copy(revocationTimestamp = None).some)))
      ()            <- Future
        .traverse(reactivateTenantAttributes) { case (id, a) =>
          tenantManagementService.updateTenantAttribute(tenant.id, id, a)
        }
        .void
      updatedTenant <- tenantManagementService.getTenant(tenant.id)
      _             <- computeAgreements(newAttributes.map(_.id))
      _             <- computeAgreements(reactivateTenantAttributes.map { case (id, _) => id })
    } yield updatedTenant
  }

  private def getAttributes(attributes: Seq[ExternalId])(implicit
    contexts: Seq[(String, String)]
  ): Future[Seq[Attribute]] =
    Future.traverse(attributes)(a => attributeRegistryManagementService.getAttributeByExternalCode(a.origin, a.value))

  private def findTenant(id: ExternalId)(implicit contexts: Seq[(String, String)]): Future[Option[DependencyTenant]] =
    tenantManagementService.getTenantByExternalId(id.toDependency).map(_.some).recover {
      case err: TenantApiError[_] if err.code == 404 => None
    }

  override def getTenant(id: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = authorize(ADMIN_ROLE, API_ROLE, M2M_ROLE, SECURITY_ROLE) {
    logger.info(s"Retrieving tenant $id")
    val result: Future[Tenant] = for {
      uuid   <- id.toFutureUUID
      tenant <- tenantManagementService.getTenant(uuid)
    } yield tenant.toApi

    onComplete(result) {
      handleApiError() orElse {
        case Success(tenant) => getTenant200(tenant)
        case Failure(ex)     =>
          logger.error(s"Error while retrieving tenant $id", ex)
          internalServerError()
      }
    }
  }

  def assertAttributeVerificationAllowed(producerId: UUID, consumerId: UUID, attributeId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit] =
    assertVerifiedAttributeOperationAllowed(
      producerId,
      consumerId,
      attributeId,
      Seq(AgreementState.PENDING, AgreementState.ACTIVE, AgreementState.SUSPENDED),
      AttributeVerificationNotAllowed(consumerId, attributeId)
    )

  def assertAttributeRevocationAllowed(producerId: UUID, consumerId: UUID, attributeId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit] = assertVerifiedAttributeOperationAllowed(
    producerId,
    consumerId,
    attributeId,
    Seq(AgreementState.PENDING, AgreementState.ACTIVE, AgreementState.SUSPENDED),
    AttributeRevocationNotAllowed(consumerId, attributeId)
  )

  def assertVerifiedAttributeOperationAllowed(
    producerId: UUID,
    consumerId: UUID,
    attributeId: UUID,
    agreementStates: Seq[AgreementState],
    error: ComponentError
  )(implicit contexts: Seq[(String, String)]): Future[Unit] = for {
    agreements <- agreementManagementService.getAgreements(producerId, consumerId, agreementStates)
    eServices  <- Future.traverse(agreements.map(_.eserviceId))(catalogManagementService.getEServiceById)
    attributeIds = eServices
      .flatMap(_.attributes.verified)
      .flatMap(attr => attr.single.map(_.id).toSeq ++ attr.group.traverse(_.map(_.id)).flatten)
      .toSet
    _ <- Future.failed(error).unlessA(attributeIds.contains(attributeId))
  } yield ()

  def handleApiError()(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): PartialFunction[Try[_], StandardRoute] = { case Failure(err: TenantApiError[_]) =>
    logger.error(s"Error received from tenant Management - ${err.responseContent}")

    err.responseContent match {
      case Some(body: String) =>
        TenantProblem.fromString(body).fold(_ => internalServerError(), problem => complete(problem.status, problem))
      case _                  => internalServerError()
    }
  }

  def addRevoker(
    verifiedAttribute: DependencyVerifiedTenantAttribute,
    now: OffsetDateTime,
    verifier: DependencyTenantVerifier
  ): DependencyVerifiedTenantAttribute =
    verifiedAttribute.copy(
      verifiedBy = verifiedAttribute.verifiedBy.filterNot(_.id == verifier.id),
      revokedBy = verifiedAttribute.revokedBy :+ DependencyTenantRevoker(
        id = verifier.id,
        verificationDate = verifier.verificationDate,
        renewal = verifier.renewal,
        expirationDate = verifier.expirationDate,
        extensionDate = verifier.extensionDate,
        revocationDate = now
      )
    )

  def internalServerError()(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): StandardRoute = {
    val problem = problemOf(StatusCodes.InternalServerError, GenericError("Error while executing the request"))
    complete(problem.status, problem)
  }
}
