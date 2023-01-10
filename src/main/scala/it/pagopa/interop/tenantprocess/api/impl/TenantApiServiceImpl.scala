package it.pagopa.interop.tenantprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.agreementmanagement.client.model.AgreementState
import it.pagopa.interop.attributeregistrymanagement.client.model.Attribute
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.jwt._
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils.getOrganizationIdFutureUUID
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.commons.utils.service.{OffsetDateTimeSupplier, UUIDSupplier}
import it.pagopa.interop.tenantmanagement.client
import it.pagopa.interop.tenantmanagement.client.invoker.{ApiError => TenantApiError}
import it.pagopa.interop.tenantmanagement.client.model.{
  TenantFeature,
  Certifier => DependencyCertifier,
  Tenant => DependencyTenant,
  TenantAttribute => DependencyTenantAttribute,
  TenantDelta => DependencyTenantDelta,
  TenantRevoker => DependencyTenantRevoker,
  TenantVerifier => DependencyTenantVerifier,
  VerifiedTenantAttribute => DependencyVerifiedTenantAttribute
}
import it.pagopa.interop.tenantprocess.api.TenantApiService
import it.pagopa.interop.tenantprocess.api.adapters.AdaptableSeed
import it.pagopa.interop.tenantprocess.api.adapters.AdaptableSeed._
import it.pagopa.interop.tenantprocess.api.adapters.ApiAdapters._
import it.pagopa.interop.tenantprocess.api.adapters.AttributeRegistryManagementAdapters._
import it.pagopa.interop.tenantprocess.api.adapters.ReadModelTenantAdapters._
import it.pagopa.interop.tenantprocess.api.adapters.TenantManagementAdapters._
import it.pagopa.interop.tenantprocess.common.readmodel.ReadModelQueries
import it.pagopa.interop.tenantprocess.error.ResponseHandlers._
import it.pagopa.interop.tenantprocess.error.TenantProcessErrors._
import it.pagopa.interop.tenantprocess.model._
import it.pagopa.interop.tenantprocess.service._

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class TenantApiServiceImpl(
  attributeRegistryManagementService: AttributeRegistryManagementService,
  tenantManagementService: TenantManagementService,
  agreementProcessService: AgreementProcessService,
  agreementManagementService: AgreementManagementService,
  catalogManagementService: CatalogManagementService,
  readModel: ReadModelService,
  uuidSupplier: UUIDSupplier,
  dateTimeSupplier: OffsetDateTimeSupplier
)(implicit ec: ExecutionContext)
    extends TenantApiService {

  private implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getProducers(name: Option[String], offset: Int, limit: Int)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerTenants: ToEntityMarshaller[Tenants],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, API_ROLE, SECURITY_ROLE) {
    val operationLabel = s"Retrieving Producers with name = $name, limit = $limit, offset = $offset"
    logger.info(operationLabel)

    val result: Future[Tenants] = ReadModelQueries
      .listProducers(name, offset, limit)(readModel)
      .map(result => Tenants(results = result.results.map(_.toApi), totalCount = result.totalCount))

    onComplete(result) {
      getProducersResponse[Tenants](operationLabel)(getProducers200)
    }
  }

  override def getConsumers(name: Option[String], offset: Int, limit: Int)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerTenants: ToEntityMarshaller[Tenants],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, API_ROLE, SECURITY_ROLE) {
    val operationLabel = s"Retrieving Consumers with name = $name, limit = $limit, offset = $offset"
    logger.info(operationLabel)

    val result: Future[Tenants] = for {
      requesterId <- getOrganizationIdFutureUUID(contexts)
      result      <- ReadModelQueries.listConsumers(name, requesterId, offset, limit)(readModel)
    } yield Tenants(results = result.results.map(_.toApi), totalCount = result.totalCount)

    onComplete(result) {
      getConsumersResponse[Tenants](operationLabel)(getConsumers200)
    }
  }

  override def updateTenant(id: String, tenantDelta: TenantDelta)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = authorize(ADMIN_ROLE) {
    val operationLabel = s"Updating Tenant $id"
    logger.info(operationLabel)

    val result: Future[Tenant] = for {
      tenantUUID       <- id.toFutureUUID
      tenantManagement <- tenantManagementService.getTenant(tenantUUID)
      selfcareId = tenantManagement.selfcareId
      features   = tenantManagement.features
      tenant <- tenantManagementService.updateTenant(tenantUUID, tenantDelta.fromAPI(selfcareId, features))
    } yield tenant.toApi

    onComplete(result) {
      updateTenantResponse[Tenant](operationLabel)(updateTenant200)
    }
  }

  override def internalUpsertTenant(seed: InternalTenantSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = authorize(INTERNAL_ROLE) {
    val operationLabel = s"Creating tenant with external id ${seed.externalId} via internal request"
    logger.info(operationLabel)

    val now: OffsetDateTime = dateTimeSupplier.get()

    val result: Future[Tenant] = for {
      existingTenant <- findTenant(seed.externalId)
      attributesIds = seed.certifiedAttributes.map(a => ExternalId(a.origin, a.code))
      tenant <- existingTenant.fold(createTenant(seed, attributesIds, now))(
        updateTenantCertifiedAttributes(attributesIds, now)
      )
    } yield tenant.toApi

    onComplete(result) {
      internalUpsertTenantResponse[Tenant](operationLabel)(internalUpsertTenant200)
    }
  }

  override def m2mUpsertTenant(seed: M2MTenantSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route =
    authorize(M2M_ROLE) {
      val operationLabel = s"Creating tenant with external id ${seed.externalId} via m2m request"
      logger.info(operationLabel)

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
        m2mUpsertTenantResponse[Tenant](operationLabel)(m2mUpsertTenant200)
      }
    }

  override def m2mRevokeAttribute(origin: String, externalId: String, code: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(M2M_ROLE) {
    val operationLabel = s"Revoking attribute $code from tenant ($origin,$externalId) via m2m request"
    logger.info(operationLabel)

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
      attributeToModify   <- tenantToModify.attributes
        .mapFilter(_.certified)
        .find(_.id == attributeIdToRevoke)
        .toFuture(CertifiedAttributeNotFoundInTenant(tenantToModify.id, origin, code))
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
      m2mRevokeAttributeResponse[Unit](operationLabel)(_ => m2mRevokeAttribute204)
    }
  }

  override def selfcareUpsertTenant(seed: SelfcareTenantSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = authorize(ADMIN_ROLE, API_ROLE, SECURITY_ROLE, INTERNAL_ROLE) {
    val operationLabel = s"Creating tenant with external id ${seed.externalId} via SelfCare request"
    logger.info(operationLabel)

    val now: OffsetDateTime = dateTimeSupplier.get()

    def updateSelfcareId(tenant: DependencyTenant): Future[DependencyTenant] = {
      def updateTenant(): Future[DependencyTenant]                     = tenantManagementService
        .updateTenant(
          tenant.id,
          DependencyTenantDelta(
            selfcareId = seed.selfcareId.some,
            features = tenant.features,
            mails = tenant.mails.map(_.toSeed)
          )
        )
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
      selfcareUpsertTenantResponse[Tenant](operationLabel)(selfcareUpsertTenant200)
    }
  }

  override def addDeclaredAttribute(seed: DeclaredTenantAttributeSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = authorize(ADMIN_ROLE) {
    val operationLabel = s"Adding declared attribute ${seed.id} to requester tenant"
    logger.info(operationLabel)

    val now: OffsetDateTime = dateTimeSupplier.get()

    def addAttribute(tenantId: UUID, seed: DeclaredTenantAttributeSeed): Future[DependencyTenant] =
      tenantManagementService.addTenantAttribute(tenantId, seed.toDependency(now))

    def updateAttribute(tenantId: UUID, attribute: DependencyTenantAttribute): Future[DependencyTenant] = for {
      declaredAttribute <- attribute.declared.toFuture(DeclaredAttributeNotFound(tenantId, seed.id.toString))
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
      addDeclaredAttributeResponse[Tenant](operationLabel)(addDeclaredAttribute200)
    }
  }

  override def revokeDeclaredAttribute(attributeId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = authorize(ADMIN_ROLE) {
    val operationLabel = s"Revoking declared attribute $attributeId to requester tenant"
    logger.info(operationLabel)

    val now: OffsetDateTime = dateTimeSupplier.get()

    val result: Future[Tenant] = for {
      requesterTenantUuid <- getOrganizationIdFutureUUID(contexts)
      _ = logger.info(s"Revoking declared attribute $attributeId to $requesterTenantUuid")
      attributeUuid     <- attributeId.toFutureUUID
      attribute         <- tenantManagementService.getTenantAttribute(requesterTenantUuid, attributeUuid)
      declaredAttribute <- attribute.declared.toFuture(
        DeclaredAttributeNotFoundInTenant(requesterTenantUuid, attributeUuid)
      )
      revokedAttribute = declaredAttribute.copy(revocationTimestamp = now.some).toTenantAttribute
      tenant <- tenantManagementService.updateTenantAttribute(requesterTenantUuid, attributeUuid, revokedAttribute)
      _      <- agreementProcessService.computeAgreementsByAttribute(requesterTenantUuid, attributeUuid)
    } yield tenant.toApi

    onComplete(result) {
      revokeDeclaredAttributeResponse[Tenant](operationLabel)(revokeDeclaredAttribute200)
    }
  }

  override def verifyVerifiedAttribute(tenantId: String, seed: VerifiedTenantAttributeSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = authorize(ADMIN_ROLE) {
    val operationLabel = s"Verifying attribute ${seed.id} to tenant $tenantId"
    logger.info(operationLabel)

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
      verifyVerifiedAttributeResponse[Tenant](operationLabel)(verifyVerifiedAttribute200)
    }
  }

  override def revokeVerifiedAttribute(tenantId: String, attributeId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = authorize(ADMIN_ROLE) {
    val operationLabel = s"Revoking attribute $attributeId to tenant $tenantId"
    logger.info(operationLabel)

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
        .toFuture(VerifiedAttributeNotFoundInTenant(targetTenantUuid, attributeUuid))
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
      revokeVerifiedAttributeResponse[Tenant](operationLabel)(revokeVerifiedAttribute200)
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
    val operationLabel = s"Retrieving tenant $id"
    logger.info(operationLabel)

    val result: Future[Tenant] = for {
      uuid   <- id.toFutureUUID
      tenant <- tenantManagementService.getTenant(uuid)
    } yield tenant.toApi

    onComplete(result) {
      getTenantResponse[Tenant](operationLabel)(getTenant200)
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
}
