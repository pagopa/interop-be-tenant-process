package it.pagopa.interop.tenantprocess.common.system

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {
  val config: Config = ConfigFactory.load()

  val serverPort: Int          = config.getInt("tenant-process.port")
  val jwtAudience: Set[String] = config.getString("tenant-process.jwt.audience").split(",").toSet.filter(_.nonEmpty)

  val tenantManagementURL: String = config.getString("tenant-process.services.tenant-management")

  val storageKind: String      = config.getString("tenant-process.storage.kind")
  val storageContainer: String = config.getString("tenant-process.storage.container")
  val storagePath: String      = config.getString("tenant-process.storage.risk-analysis-path")

  require(jwtAudience.nonEmpty, "Audience cannot be empty")
}
