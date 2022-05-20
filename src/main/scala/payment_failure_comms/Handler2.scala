package payment_failure_comms

import com.amazonaws.services.lambda.runtime.LambdaLogger
import payment_failure_comms.models.{Config, IdapiConfig, PaymentFailureRecord, PaymentFailureRecordWithBrazeId}
import payment_failure_comms.service._
import zio._

object Handler2 extends ZIOAppDefault {

  private val program =
    for {
      config <- ZIO.service[Config]
      records <- Salesforce.fetchPaymentFailureRecords
      augmentedRecords <- augmentRecordsWithBrazeId(findBrazeId(config.idapi, ConsoleLogger()))(records)
      _ <- ZIO.foreachDiscard(augmentedRecords)(record => Console.printLine(record))
    } yield ()

  private def findBrazeId(idapiConfig: IdapiConfig, logger: LambdaLogger)(
      record: PaymentFailureRecord
  ): Option[String] =
    record.Contact__r.IdentityID__c match {
      case Some(identityId) =>
        IdapiConnector.getBrazeId(idapiConfig, logger)(identityId) match {
          case Left(_)        => None
          case Right(brazeId) => Some(brazeId)
        }
      case None => Some(record.Contact__c)
    }

  private case class PartitionedRecords(
      withBrazeId: Seq[PaymentFailureRecordWithBrazeId],
      withoutBrazeId: Seq[PaymentFailureRecord]
  )

  private def augmentRecordsWithBrazeId(
      findBrazeId: PaymentFailureRecord => Option[String]
  )(records: Seq[PaymentFailureRecord]) = {
    val w = for {
      x <- ZIO.foldLeft(records)(PartitionedRecords(Nil, Nil))((acc, record) =>
        ZIO.attempt(findBrazeId(record)) map {
          case None => acc.copy(withoutBrazeId = acc.withoutBrazeId :+ record)
          case Some(brazeId) =>
            acc.copy(withBrazeId = acc.withBrazeId :+ PaymentFailureRecordWithBrazeId(record, brazeId))
        }
      )
    } yield x
    w
  }

  override def run: ZIO[ZIOAppArgs, Any, Any] =
    program.provide(LoggingLive.layer(ConsoleLogger()), ConfigLoad.layer, SalesforceLive.layer)
}
