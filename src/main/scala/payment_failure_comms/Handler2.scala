package payment_failure_comms

import payment_failure_comms.Handler.{PartitionedRecords, augmentRecordsWithBrazeId, findBrazeId}
import payment_failure_comms.models.{PaymentFailureRecord, PaymentFailureRecordWithBrazeId}
import payment_failure_comms.service.{ConfigLoad, Idapi, LoggingLive, Salesforce, SalesforceLive}
import zio._

object Handler2 extends ZIOAppDefault {

  private val program =
    for {
      records <- Salesforce.fetchPaymentFailureRecords
      augmentedRecords = augmentRecordsWithBrazeId(findBrazeId(config.idapi, logger))(records)
      _ <- ZIO.foreachDiscard(records)(record => Console.printLine(record))
    } yield ()

  private def augmentRecordsWithBrazeId(records: Seq[PaymentFailureRecord]) = {
    def r(record: PaymentFailureRecord) =
      record match {
        case None => acc.copy(withoutBrazeId = acc.withoutBrazeId :+ record)
        case Some(brazeId) =>
          acc.copy(withBrazeId = acc.withBrazeId :+ PaymentFailureRecordWithBrazeId(record, brazeId))
      }
    for {
      x <- ZIO.foldLeft(records)(Nil, Nil)((acc, record) =>
        Idapi.fetchBrazeId(record) match {
          case None => acc.copy(withoutBrazeId = acc.withoutBrazeId :+ record)
          case Some(brazeId) =>
            acc.copy(withBrazeId = acc.withBrazeId :+ PaymentFailureRecordWithBrazeId(record, brazeId))
        }
      )
    } yield x
  }

  override def run: ZIO[ZIOAppArgs, Any, Any] =
    program.provide(LoggingLive.layer(ConsoleLogger()), ConfigLoad.layer, SalesforceLive.layer)
}
