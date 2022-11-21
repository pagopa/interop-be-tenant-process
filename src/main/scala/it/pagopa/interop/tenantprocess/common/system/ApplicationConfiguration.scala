package it.pagopa.interop.tenantprocess.common.system

import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig

object ApplicationConfiguration {
  val config: Config = ConfigFactory.load()

  val serverPort: Int          = config.getInt("tenant-process.port")
  val jwtAudience: Set[String] = config.getString("tenant-process.jwt.audience").split(",").toSet.filter(_.nonEmpty)

  val attributeRegistryManagementURL: String = config.getString("tenant-process.services.attribute-registry-management")
  val agreementProcessURL: String            = config.getString("tenant-process.services.agreement-process")
  val agreementManagementURL: String         = config.getString("tenant-process.services.agreement-management")
  val catalogManagementURL: String           = config.getString("tenant-process.services.catalog-management")
  val tenantManagementURL: String            = config.getString("tenant-process.services.tenant-management")

  val readModelConfig: ReadModelConfig = {
    val connectionString: String = config.getString("tenant-process.read-model.db.connection-string")
    val dbName: String           = config.getString("tenant-process.read-model.db.name")

    ReadModelConfig(connectionString, dbName)
  }

  require(jwtAudience.nonEmpty, "Audience cannot be empty")
}
