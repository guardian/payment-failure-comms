package payment_failure_comms.services

import payment_failure_comms.models.{Config, ConfigFailure}
import zio.{Has, IO, Layer, UIO, URIO, ZIO, ZIOAppArgs, ZIOAppDefault}

trait Configuration {
  def get: UIO[Config]
}

object Configuration {
  val get: URIO[Has[Configuration], Config] = URIO.serviceWith(_.get)
}

object ConfigurationLive {
  val effect: IO[ConfigFailure, Configuration] =
    IO.fromEither(Config())
      .debug("*** loading config ...")
      .map(config =>
        new Configuration {
          def get: UIO[Config] = UIO.succeed(config)
        }
      )
  val layer: Layer[ConfigFailure, Has[Configuration]] = effect.toLayer
}

object Client extends ZIOAppDefault {
  def run: ZIO[zio.ZEnv with Has[ZIOAppArgs], Any, Any] = (for {
    config <- Configuration.get
    _ <- zio.Console.printLine(())
    _ <- zio.Console.printLine(config.salesforce.toString)
    _ <- zio.Console.printLine(())
    _ <- zio.Console.printLine(config.braze.toString)
  } yield ()).injectCustom(ConfigurationLive.layer)
}
