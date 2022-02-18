package payment_failure_comms.services

/*
import payment_failure_comms.models.{Config, Failure}
import zio.{Has, IO, Layer, UIO, URIO, ZIO, ZIOAppArgs, ZIOAppDefault}

trait Configuration {
  def get: UIO[Config]
}

object Configuration {
  val get: URIO[Has[Configuration], Config] = URIO.serviceWith(_.get)
}

object ConfigurationLive {
  val effect: IO[Failure, Configuration] =
    IO.fromEither(Config())
      .debug("*** loading config ...")
      .map(config =>
        new Configuration {
          def get: UIO[Config] = UIO.succeed(config)
        }
      )
  val layer: Layer[Failure, Has[Configuration]] = effect.toLayer
}

/*
case class MyConfig(ldap: String, port: Int, dburl: String)

val myConfig: ConfigDescriptor[MyConfig] =
  (string("LDAP") |@| int("PORT") |@| string("DB_URL"))(MyConfig.apply, MyConfig.unapply)

val result: Layer[ReadError[String], Has[MyConfig]] = System.live >>> ZConfig.fromSystemEnv(myConfig)

zio.Runtime.default.unsafeRun(result.launch)
 */
object Client extends ZIOAppDefault {
  def run: ZIO[zio.ZEnv with Has[ZIOAppArgs], Any, Any] = (for {
    config <- ZIO.serviceWith[Configuration](_.get)
    _ <- zio.Console.printLine(())
    _ <- zio.Console.printLine(config.salesforce.toString)
    _ <- zio.Console.printLine(())
    _ <- zio.Console.printLine(config.braze.toString)
  } yield ()).injectCustom(ConfigurationLive.layer)
}
 */
