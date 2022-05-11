package payment_failure_comms

import payment_failure_comms.service.{ConfigLoad, LoggingLive, Salesforce, SalesforceLive}
import zio._

object Handler2 extends ZIOAppDefault {

  private val program =
    for {
      records <- Salesforce.fetchPaymentFailureRecords
      _ <- ZIO.foreachDiscard(records)(record => Console.printLine(record))
    } yield ()

  override def run: ZIO[ZIOAppArgs, Any, Any] =
    program.provide(LoggingLive.layer(ConsoleLogger()), ConfigLoad.layer, SalesforceLive.layer)
}
