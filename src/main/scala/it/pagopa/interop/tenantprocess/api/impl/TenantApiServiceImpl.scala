package it.pagopa.interop.tenantprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.agreementmanagement.model.{agreement => AgreementPersistentModel}
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.jwt._
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils.{getOrganizationIdFutureUUID, getUserRolesListFuture}
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.{ComponentError, GenericComponentErrors}
import it.pagopa.interop.commons.utils.service.{OffsetDateTimeSupplier, UUIDSupplier}
import it.pagopa.interop.attributeregistrymanagement.model.persistence.attribute._
import it.pagopa.interop.tenantmanagement.client.model.{
  TenantFeature => DependencyTenantFeature,
  Certifier => DependencyCertifier,
  ExternalId => DependencyExternalId,
  Tenant => DependencyTenant,
  TenantAttribute => DependencyTenantAttribute,
  TenantDelta => DependencyTenantDelta,
  TenantKind => DependencyTenantKind,
  TenantRevoker => DependencyTenantRevoker,
  TenantVerifier => DependencyTenantVerifier,
  CertifiedTenantAttribute => DependencyCertifiedTenantAttribute,
  VerifiedTenantAttribute => DependencyVerifiedTenantAttribute
}
import it.pagopa.interop.tenantprocess.api.TenantApiService
import it.pagopa.interop.tenantprocess.api.adapters.AdaptableSeed
import it.pagopa.interop.tenantprocess.api.adapters.AdaptableSeed._
import it.pagopa.interop.tenantprocess.api.adapters.ApiAdapters._
import it.pagopa.interop.tenantprocess.api.adapters.AttributeRegistryManagementAdapters._
import it.pagopa.interop.tenantprocess.api.adapters.ReadModelTenantAdapters._
import it.pagopa.interop.tenantprocess.api.adapters.TenantManagementAdapters._
import it.pagopa.interop.tenantprocess.common.readmodel._
import it.pagopa.interop.tenantprocess.error.ResponseHandlers._
import it.pagopa.interop.tenantprocess.error.TenantProcessErrors._
import it.pagopa.interop.tenantprocess.model._
import it.pagopa.interop.tenantprocess.service._

import java.time.{Duration, OffsetDateTime}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import it.pagopa.interop.catalogmanagement.model.SingleAttribute
import it.pagopa.interop.catalogmanagement.model.GroupAttribute
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant

final case class TenantApiServiceImpl(
  tenantManagementService: TenantManagementService,
  agreementProcessService: AgreementProcessService,
  readModel: ReadModelService,
  uuidSupplier: UUIDSupplier,
  dateTimeSupplier: OffsetDateTimeSupplier
)(implicit ec: ExecutionContext)
    extends TenantApiService {

  // Enti Pubblici
  private val PUBLIC_ADMINISTRATIONS_IDENTIFIER: String           = "IPA"
  // Stazioni Appaltanti Gestori di Pubblici Servizi
  private val CONTRACT_AUTHORITY_PUBLIC_SERVICES_MANAGERS: String = "SAG"
  // Gestori di Pubblici Servizi
  private val PUBLIC_SERVICES_MANAGERS: String                    = "L37"

  private implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getProducers(name: Option[String], offset: Int, limit: Int)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerTenants: ToEntityMarshaller[Tenants],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, API_ROLE, SECURITY_ROLE, SUPPORT_ROLE) {
    val operationLabel = s"Retrieving Producers with name = $name, limit = $limit, offset = $offset"
    logger.info(operationLabel)

    val result: Future[Tenants] = TenantReadModelQueries
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
  ): Route = authorize(ADMIN_ROLE, API_ROLE, SECURITY_ROLE, SUPPORT_ROLE) {
    val operationLabel = s"Retrieving Consumers with name = $name, limit = $limit, offset = $offset"
    logger.info(operationLabel)

    val result: Future[Tenants] = for {
      requesterId <- getOrganizationIdFutureUUID(contexts)
      result      <- TenantReadModelQueries.listConsumers(name, requesterId, offset, limit)(readModel)
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
      _                <- assertResourceAllowed(tenantUUID)
      tenantManagement <- getPersistentTenant(tenantUUID).map(_.toManagement)
      tenantKind       <- tenantManagement.kind.fold(
        getTenantKindLoadingCertifiedAttributes(tenantManagement.attributes, tenantManagement.externalId)
      )(Future.successful)
      tenant           <- tenantManagementService.updateTenant(
        tenantUUID,
        tenantDelta.fromAPI(tenantManagement.selfcareId, tenantManagement.features, tenantKind)
      )
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
      tenant <- existingTenant.fold(
        createTenant(seed, attributesIds, now, getTenantKind(attributesIds, seed.externalId).fromAPI)
      )(updateTenantCertifiedAttributes(attributesIds, now))
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
        requesterTenant     <- getPersistentTenant(requesterTenantUuid).map(_.toManagement)
        maybeCertifier = requesterTenant.features
          .collectFirst { case DependencyTenantFeature(Some(certifier)) => certifier }
        certifier <- maybeCertifier.toFuture(TenantIsNotACertifier(requesterTenantUuid))
      } yield certifier

      val result: Future[Tenant] = for {
        certifier      <- validateCertifierTenant
        existingTenant <- findTenant(seed.externalId)
        attributesId = seed.certifiedAttributes.map(a => ExternalId(certifier.certifierId, a.code))
        tenant <- existingTenant.fold(
          createTenant(seed, attributesId, now, getTenantKind(attributesId, seed.externalId).fromAPI)
        )(updateTenantCertifiedAttributes(attributesId, now))
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
      requesterTenant     <- getPersistentTenant(requesterTenantUuid).map(_.toManagement)
      certifierId         <- requesterTenant.features
        .collectFirstSome(_.certifier.map(_.certifierId))
        .toFuture(TenantIsNotACertifier(requesterTenantUuid))
      result              <- revokeCertifiedAttribute(
        tenantOrigin = origin,
        tenantExternalId = externalId,
        attributeOrigin = certifierId,
        attributeExternalId = code
      )
      (tenant, attribute) = result
      _ <- agreementProcessService.computeAgreementsByAttribute(tenant.id, attribute.id)
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

    def updateSelfcareId(tenant: DependencyTenant, kind: DependencyTenantKind): Future[DependencyTenant] = {
      def updateTenant(): Future[DependencyTenant]                     = tenantManagementService
        .updateTenant(
          tenant.id,
          DependencyTenantDelta(
            selfcareId = seed.selfcareId.some,
            features = tenant.features,
            mails = tenant.mails.map(_.toSeed),
            kind = kind
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
      _              <- existingTenant.traverse(t => assertResourceAllowed(t.id))
      tenant         <- existingTenant
        .fold(createTenant(seed, Nil, now, getTenantKind(Nil, seed.externalId).fromAPI))(Future.successful)
      tenantKind     <- getTenantKindLoadingCertifiedAttributes(tenant.attributes, tenant.externalId)
      updatedTenant  <- updateSelfcareId(tenant, tenantKind)
    } yield updatedTenant.toApi

    onComplete(result) {
      selfcareUpsertTenantResponse[Tenant](operationLabel)(selfcareUpsertTenant200)
    }
  }

  override def internalRevokeCertifiedAttribute(
    tenantOrigin: String,
    tenantExternalId: String,
    attributeOrigin: String,
    attributeExternalId: String
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    authorize(INTERNAL_ROLE) {
      val operationLabel =
        s"Revoking certified attribute ($attributeOrigin/$attributeExternalId) from tenant ($tenantOrigin/$tenantExternalId)"
      logger.info(operationLabel)

      val result: Future[Unit] = for {
        result <- revokeCertifiedAttribute(
          tenantOrigin = tenantOrigin,
          tenantExternalId = tenantExternalId,
          attributeOrigin = attributeOrigin,
          attributeExternalId = attributeExternalId
        )
        (tenant, attribute) = result
        _ <- agreementProcessService.computeAgreementsByAttribute(tenant.id, attribute.id)
      } yield ()

      onComplete(result) {
        internalRevokeCertifiedAttributeResponse[Unit](operationLabel)(_ => internalRevokeCertifiedAttribute204)
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

    def upsertAttribute(tenant: PersistentTenant, seed: DeclaredTenantAttributeSeed): Future[DependencyTenant] = {
      val maybeAttribute: Option[DependencyTenantAttribute] = tenant.attributes
        .find(_.id == seed.id)
        .map(_.toManagement)
      maybeAttribute.fold(addAttribute(tenant.id, seed))(updateAttribute(tenant.id, _))
    }

    val result: Future[Tenant] = for {
      requesterTenantUuid <- getOrganizationIdFutureUUID(contexts)
      _ = logger.info(s"Adding declared attribute ${seed.id} to $requesterTenantUuid")
      persistentTenant <- getPersistentTenant(requesterTenantUuid)
      tenant           <- upsertAttribute(persistentTenant, seed)
      _                <- agreementProcessService.computeAgreementsByAttribute(requesterTenantUuid, seed.id)
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
      tenant            <- getPersistentTenant(requesterTenantUuid)
      attribute         <- tenant.attributes
        .find(_.id == attributeUuid)
        .toFuture(TenantAttributeNotFound(tenant.id, attributeUuid))
      declaredAttribute <- attribute.toManagement.declared.toFuture(
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
      targetTenant <- getPersistentTenant(targetTenantUuid).map(_.toManagement)
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

  override def updateVerifiedAttribute(tenantId: String, attributeId: String, seed: UpdateVerifiedTenantAttributeSeed)(
    implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = authorize(ADMIN_ROLE) {
    val operationLabel = s"Update attribute ${attributeId} to tenant $tenantId"
    logger.info(operationLabel)

    val now: OffsetDateTime = dateTimeSupplier.get()

    val result: Future[Tenant] = for {
      requesterUuid  <- getOrganizationIdFutureUUID(contexts)
      tenantUuid     <- tenantId.toFutureUUID
      attributeUuiId <- attributeId.toFutureUUID
      _              <- seed.expirationDate match {
        case Some(value) if (value.isBefore(now)) => Future.failed(ExpirationDateCannotBeInThePast(value))
        case _                                    => Future.successful(())
      }
      tenant         <- getPersistentTenant(tenantUuid).map(_.toManagement)
      attribute      <- tenant.attributes
        .flatMap(_.verified)
        .find(_.id == attributeUuiId)
        .toFuture(VerifiedAttributeNotFoundInTenant(tenantUuid, attributeUuiId))
      _              <- attribute.verifiedBy
        .find(_.id == requesterUuid)
        .toFuture(OrganizationNotFoundInVerifiers(requesterUuid, tenantUuid, attribute.id))
      updatedTenant  <- tenantManagementService.updateTenantAttribute(
        tenantUuid,
        attributeUuiId,
        seed.toUpdateDependency(attributeUuiId, now, requesterUuid, attribute)
      )
    } yield updatedTenant.toApi

    onComplete(result) {
      updateVerifiedAttributeResponse[Tenant](operationLabel)(updateVerifiedAttribute200)
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
      targetTenant  <- getPersistentTenant(targetTenantUuid).map(_.toManagement)
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

  private def createTenant[T: AdaptableSeed](
    seed: T,
    attributes: Seq[ExternalId],
    timestamp: OffsetDateTime,
    kind: DependencyTenantKind
  )(implicit contexts: Seq[(String, String)]): Future[DependencyTenant] =
    for {
      attributes <- getAttributes(attributes)
      dependencyAttributes = attributes.map(_.toCertifiedSeed(timestamp))
      tenantId             = uuidSupplier.get()
      tenant <- tenantManagementService.createTenant(toDependency(seed, tenantId, dependencyAttributes, kind))
    } yield tenant

  private def updateTenantCertifiedAttributes(attributes: Seq[ExternalId], timestamp: OffsetDateTime)(
    tenant: DependencyTenant
  )(implicit contexts: Seq[(String, String)]): Future[DependencyTenant] = {
    def computeAgreements(attributesIds: Seq[UUID]): Future[Seq[Unit]] =
      Future.traverse(attributesIds)(agreementProcessService.computeAgreementsByAttribute(tenant.id, _))

    def updateTenant(tenant: DependencyTenant, kind: DependencyTenantKind): Future[DependencyTenant] =
      tenantManagementService
        .updateTenant(
          tenant.id,
          DependencyTenantDelta(
            selfcareId = tenant.selfcareId,
            features = tenant.features,
            mails = tenant.mails.map(_.toSeed),
            kind = kind
          )
        )

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
        .map(c => (c.id, DependencyTenantAttribute(certified = c.copy(revocationTimestamp = None).some)))
      ()            <- Future
        .traverse(reactivateTenantAttributes) { case (id, a) =>
          tenantManagementService.updateTenantAttribute(tenant.id, id, a)
        }
        .void
      updatedTenant <- getPersistentTenant(tenant.id).map(_.toManagement)
      tenantKind    <- getTenantKindLoadingCertifiedAttributes(tenant.attributes, tenant.externalId)
      updatedTenant <- updatedTenant.kind match {
        case Some(x) if (x == tenantKind) => Future.successful(updatedTenant)
        case _                            => updateTenant(updatedTenant, tenantKind)
      }
      _             <- computeAgreements(newAttributes.map(_.id))
      _             <- computeAgreements(reactivateTenantAttributes.map { case (id, _) => id })
    } yield updatedTenant
  }

  private def getTenantKind(attributes: Seq[ExternalId], externalId: ExternalId): TenantKind = {
    externalId.origin match {
      case PUBLIC_ADMINISTRATIONS_IDENTIFIER
          if (attributes.exists(attr =>
            attr.origin == PUBLIC_ADMINISTRATIONS_IDENTIFIER && (attr.value == PUBLIC_SERVICES_MANAGERS || attr.value == CONTRACT_AUTHORITY_PUBLIC_SERVICES_MANAGERS)
          )) =>
        TenantKind.GSP
      case PUBLIC_ADMINISTRATIONS_IDENTIFIER => TenantKind.PA
      case _                                 => TenantKind.PRIVATE
    }
  }

  private def getTenantKindLoadingCertifiedAttributes(
    attributes: Seq[DependencyTenantAttribute],
    externalId: DependencyExternalId
  ): Future[DependencyTenantKind] = {

    def getCertifiedAttributesIds(attributes: Seq[DependencyTenantAttribute]): Seq[UUID] = for {
      attributes <- attributes
      certified  <- attributes.certified
    } yield certified.id

    def convertAttributes(attributes: Seq[PersistentAttribute]): Seq[ExternalId] = for {
      attributes <- attributes
      origin     <- attributes.origin
      code       <- attributes.code
    } yield ExternalId(origin, code)

    def getDependencyAttributes(attributes: Seq[UUID]): Future[Seq[PersistentAttribute]] =
      Future.traverse(attributes)(a =>
        AttributeRegistryReadModelQueries.getAttributeById(a)(readModel).flatMap { attr =>
          attr.toFuture(RegistryAttributeIdNotFound(a))
        }
      )

    for {
      attributesIds <- Future.successful(getCertifiedAttributesIds(attributes))
      attrs         <- getDependencyAttributes(attributesIds)
      extIds     = convertAttributes(attrs)
      tenantKind = getTenantKind(extIds, externalId.toApi)
    } yield tenantKind.fromAPI
  }

  private def getAttributes(attributes: Seq[ExternalId]): Future[Seq[PersistentAttribute]] =
    Future.traverse(attributes)(a =>
      AttributeRegistryReadModelQueries.getAttributeByExternalCode(a.origin, a.value)(readModel).flatMap { attr =>
        attr.toFuture(RegistryAttributeNotFound(a.origin, a.value))
      }
    )

  private def getPersistentTenant(tenantId: UUID): Future[PersistentTenant] = for {
    pt     <- TenantReadModelQueries.getTenant(tenantId)(readModel)
    tenant <- pt.toFuture(TenantByIdNotFound(tenantId))
  } yield tenant

  private def getPersistentTenant(origin: String, value: String): Future[PersistentTenant] = for {
    pt     <- TenantReadModelQueries.getTenantByExternalId(origin, value)(readModel)
    tenant <- pt.toFuture(TenantNotFound(origin, value))
  } yield tenant

  private def findTenant(id: ExternalId): Future[Option[DependencyTenant]] = for {
    tenant <- TenantReadModelQueries.getTenantByExternalId(id.origin, id.value)(readModel)
  } yield tenant.map(_.toManagement)

  override def getTenant(id: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = authorize(ADMIN_ROLE, API_ROLE, M2M_ROLE, SECURITY_ROLE, SUPPORT_ROLE) {
    val operationLabel = s"Retrieving tenant $id"
    logger.info(operationLabel)

    val result: Future[Tenant] = for {
      uuid   <- id.toFutureUUID
      tenant <- getPersistentTenant(uuid)
    } yield tenant.toApi

    onComplete(result) {
      getTenantResponse[Tenant](operationLabel)(getTenant200)
    }
  }

  private def revokeCertifiedAttribute(
    tenantOrigin: String,
    tenantExternalId: String,
    attributeOrigin: String,
    attributeExternalId: String
  )(implicit contexts: Seq[(String, String)]): Future[(DependencyTenant, DependencyCertifiedTenantAttribute)] = for {
    tenantToModify      <- getPersistentTenant(tenantOrigin, tenantExternalId)
      .map(_.toManagement)
    attributeIdToRevoke <- AttributeRegistryReadModelQueries
      .getAttributeByExternalCode(attributeOrigin, attributeExternalId)(readModel)
      .flatMap(_.toFuture(RegistryAttributeNotFound(attributeOrigin, attributeExternalId)).map(_.id))
    attributeToModify   <- tenantToModify.attributes
      .mapFilter(_.certified)
      .find(_.id == attributeIdToRevoke)
      .toFuture(CertifiedAttributeNotFoundInTenant(tenantToModify.id, attributeOrigin, attributeExternalId))
    modifiedAttribute = attributeToModify.copy(revocationTimestamp = dateTimeSupplier.get().some)
    updatedTenant <- tenantManagementService
      .updateTenantAttribute(
        tenantToModify.id,
        attributeToModify.id,
        DependencyTenantAttribute(certified = modifiedAttribute.some)
      )
    tenantKind    <- getTenantKindLoadingCertifiedAttributes(updatedTenant.attributes, updatedTenant.externalId)
    updatedTenant <- updatedTenant.kind match {
      case Some(x) if (x == tenantKind) => Future.successful(updatedTenant)
      case _                            =>
        tenantManagementService.updateTenant(
          updatedTenant.id,
          DependencyTenantDelta(
            selfcareId = updatedTenant.selfcareId,
            features = updatedTenant.features,
            mails = updatedTenant.mails.map(_.toSeed),
            kind = tenantKind
          )
        )
    }
  } yield (updatedTenant, attributeToModify)

  private def assertAttributeVerificationAllowed(producerId: UUID, consumerId: UUID, attributeId: UUID): Future[Unit] =
    assertVerifiedAttributeOperationAllowed(
      producerId,
      consumerId,
      attributeId,
      Seq(AgreementPersistentModel.Pending, AgreementPersistentModel.Active, AgreementPersistentModel.Suspended),
      AttributeVerificationNotAllowed(consumerId, attributeId)
    )

  private def assertAttributeRevocationAllowed(producerId: UUID, consumerId: UUID, attributeId: UUID): Future[Unit] =
    assertVerifiedAttributeOperationAllowed(
      producerId,
      consumerId,
      attributeId,
      Seq(AgreementPersistentModel.Pending, AgreementPersistentModel.Active, AgreementPersistentModel.Suspended),
      AttributeRevocationNotAllowed(consumerId, attributeId)
    )

  private def getAgreements(
    producerId: UUID,
    consumerId: UUID,
    agreementStates: Seq[AgreementPersistentModel.PersistentAgreementState],
    offset: Int,
    limit: Int
  )(implicit ec: ExecutionContext): Future[Seq[AgreementPersistentModel.PersistentAgreement]] = {
    AgreementReadModelQueries.getAgreements(producerId, consumerId, agreementStates, offset, limit)(readModel)
  }

  private def getAllAgreements(
    producerId: UUID,
    consumerId: UUID,
    agreementStates: Seq[AgreementPersistentModel.PersistentAgreementState]
  )(implicit ec: ExecutionContext): Future[Seq[AgreementPersistentModel.PersistentAgreement]] = {

    def getAgreementsFrom(offset: Int): Future[Seq[AgreementPersistentModel.PersistentAgreement]] =
      getAgreements(
        producerId = producerId,
        consumerId = consumerId,
        agreementStates = agreementStates,
        limit = 50,
        offset = offset
      )

    def go(start: Int)(
      as: Seq[AgreementPersistentModel.PersistentAgreement]
    ): Future[Seq[AgreementPersistentModel.PersistentAgreement]] =
      getAgreementsFrom(start).flatMap(esec =>
        if (esec.size < 50) Future.successful(as ++ esec) else go(start + 50)(as ++ esec)
      )

    go(0)(Nil)
  }

  private def assertVerifiedAttributeOperationAllowed(
    producerId: UUID,
    consumerId: UUID,
    attributeId: UUID,
    agreementStates: Seq[AgreementPersistentModel.PersistentAgreementState],
    error: ComponentError
  ): Future[Unit] = for {
    agreements <- getAllAgreements(producerId, consumerId, agreementStates)
    descriptorIds = agreements.map(_.descriptorId)
    eServices <- Future.traverse(agreements.map(_.eserviceId))(id =>
      CatalogReadModelQueries.getEServiceById(id)(readModel).flatMap { e =>
        e.toFuture(EServiceNotFound(id))
      }
    )
    attributeIds = eServices
      .flatMap(_.descriptors.filter(d => descriptorIds.contains(d.id)))
      .flatMap(_.attributes.verified)
      .flatMap(attr =>
        attr match {
          case SingleAttribute(single) => Seq(single.id)
          case GroupAttribute(group)   => group.map(_.id)
        }
      )
      .toSet
    _ <- Future.failed(error).unlessA(attributeIds.contains(attributeId))
  } yield ()

  private def addRevoker(
    verifiedAttribute: DependencyVerifiedTenantAttribute,
    now: OffsetDateTime,
    verifier: DependencyTenantVerifier
  ): DependencyVerifiedTenantAttribute =
    verifiedAttribute.copy(
      verifiedBy = verifiedAttribute.verifiedBy.filterNot(_.id == verifier.id),
      revokedBy = verifiedAttribute.revokedBy :+ DependencyTenantRevoker(
        id = verifier.id,
        verificationDate = verifier.verificationDate,
        expirationDate = verifier.expirationDate,
        extensionDate = verifier.extensionDate,
        revocationDate = now
      )
    )

  private def assertRequesterAllowed(resourceId: UUID)(requesterId: UUID): Future[Unit] =
    Future.failed(GenericComponentErrors.OperationForbidden).unlessA(resourceId == requesterId)

  private def assertResourceAllowed(resourceId: UUID)(implicit contexts: Seq[(String, String)]): Future[Unit] = for {
    roles <- getUserRolesListFuture(contexts)
    _     <- (getOrganizationIdFutureUUID(contexts) >>= assertRequesterAllowed(resourceId)).unlessA(
      roles.contains(INTERNAL_ROLE)
    )
  } yield ()

  override def updateVerifiedAttributeExtensionDate(tenantId: String, attributeId: String, verifierId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant]
  ): Route = authorize(INTERNAL_ROLE) {
    val operationLabel = s"Update extension date of attribute ${attributeId} for tenant $tenantId"
    logger.info(operationLabel)

    val result: Future[Tenant] = for {
      verifierUuid   <- verifierId.toFutureUUID
      tenantUuid     <- tenantId.toFutureUUID
      attributeUuid  <- attributeId.toFutureUUID
      tenant         <- getPersistentTenant(tenantUuid).map(_.toManagement)
      attribute      <- tenant.attributes
        .flatMap(_.verified)
        .find(_.id == attributeUuid)
        .toFuture(VerifiedAttributeNotFoundInTenant(tenantUuid, attributeUuid))
      oldVerifier    <- attribute.verifiedBy
        .find(_.id == verifierUuid)
        .toFuture(OrganizationNotFoundInVerifiers(verifierUuid, tenantUuid, attribute.id))
      expirationDate <- oldVerifier.expirationDate.toFuture(
        ExpirationDateNotFoundInVerifier(tenantUuid, attribute.id, oldVerifier.id)
      )
      extensionDate = oldVerifier.extensionDate.getOrElse(expirationDate)
      updatedTenant <- tenantManagementService.updateTenantAttribute(
        tenantUuid,
        attributeUuid,
        DependencyTenantAttribute(
          declared = None,
          certified = None,
          verified = DependencyVerifiedTenantAttribute(
            id = attributeUuid,
            assignmentTimestamp = attribute.assignmentTimestamp,
            verifiedBy = attribute.verifiedBy.filterNot(_.id == verifierUuid) :+
              DependencyTenantVerifier(
                id = verifierUuid,
                verificationDate = oldVerifier.verificationDate,
                expirationDate = oldVerifier.expirationDate,
                extensionDate = extensionDate.plus(Duration.between(oldVerifier.verificationDate, expirationDate)).some
              ),
            revokedBy = attribute.revokedBy
          ).some
        )
      )
    } yield updatedTenant.toApi

    onComplete(result) {
      updateVerifiedAttributeExtensionDateResponse[Tenant](operationLabel)(updateVerifiedAttributeExtensionDate200)
    }
  }
}
