package it.pagopa.interop.tenantprocess.server.impl

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, DispatcherSelector}
import akka.http.scaladsl.Http
import akka.management.scaladsl.AkkaManagement
import buildinfo.BuildInfo
import cats.syntax.all._
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.logging.renderBuildInfo
import it.pagopa.interop.commons.utils.CORSSupport
import it.pagopa.interop.tenantprocess.common.system.ApplicationConfiguration
import it.pagopa.interop.tenantprocess.server.Controller

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Main extends App with CORSSupport with Dependencies {

  private val logger: Logger = Logger(this.getClass)

  ActorSystem[Nothing](
    Behaviors.setup[Nothing] { context =>
      implicit val actorSystem: ActorSystem[_]        = context.system
      implicit val executionContext: ExecutionContext = actorSystem.executionContext

      // TODO Remember to modify ApiInvoker to use a configurable EC in the services that depend on this
      val selector: DispatcherSelector         = DispatcherSelector.fromConfig("futures-dispatcher")
      val blockingEc: ExecutionContextExecutor = actorSystem.dispatchers.lookup(selector)

      AkkaManagement.get(actorSystem.classicSystem).start()

      logger.info(renderBuildInfo(BuildInfo))

      val serverBinding: Future[Http.ServerBinding] = for {
        jwtReader <- jwtValidator()
        controller = new Controller(healthApi, tenantApi(jwtReader, blockingEc), validationExceptionToRoute.some)(
          actorSystem.classicSystem
        )
        binding <- Http()
          .newServerAt("0.0.0.0", ApplicationConfiguration.serverPort)
          .bind(corsHandler(controller.routes))
      } yield binding

      serverBinding.onComplete {
        case Success(b) =>
          logger.info(s"Started server at ${b.localAddress.getHostString}:${b.localAddress.getPort}")
        case Failure(e) =>
          actorSystem.terminate()
          logger.error("Startup error: ", e)
      }

      Behaviors.empty[Nothing]
    },
    BuildInfo.name
  )
}
