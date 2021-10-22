package payment_failure_comms.services

import payment_failure_comms.models.Config
import zio.{IO, UIO}

trait Configuration {
  def get: UIO[Config]
}

case class ConfigurationLive() extends Configuration {
  val config = { IO.fromEither(Config()) }.debug("*** loading config ...")
  def get: UIO[Config] = config
}
