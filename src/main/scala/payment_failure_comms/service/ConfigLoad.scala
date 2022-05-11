package payment_failure_comms.service

import payment_failure_comms.models.{Config, ConfigFailure}
import zio._

object ConfigLoad {
  val layer: ZLayer[Any, ConfigFailure, Config] = ZLayer.fromZIO(ZIO.fromEither(Config()))
}
