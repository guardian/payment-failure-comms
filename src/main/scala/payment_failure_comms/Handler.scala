package payment_failure_comms

import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger}
import payment_failure_comms.models._

object Handler {

  def handleRequest(context: Context): Unit = program(context.getLogger)

  private case class PartitionedRecords(
      withBrazeId: Seq[PaymentFailureRecordWithBrazeId],
      withoutBrazeId: Seq[PaymentFailureRecord]
  )

  def program(logger: LambdaLogger): Unit = {
    (for {
      config <- Config()
      sfConnector <- SalesforceConnector(config.salesforce, logger)

      records <- sfConnector.getRecordsToProcess()
      augmentedRecords = augmentRecordsWithBrazeId(config.idapi, logger)(records)

      currentEventsRequest = BrazeUserRequest.fromPaymentFailureRecords(augmentedRecords.withBrazeId)
      currentEventsResponse <- BrazeConnector.fetchCustomEvents(config.braze, logger)(currentEventsRequest)
      brazeRequest <- BrazeTrackRequest(augmentedRecords.withBrazeId, config.braze.zuoraAppId, currentEventsResponse)
      brazeResult = BrazeConnector.sendCustomEvents(config.braze, logger)(brazeRequest)

      updateRecordsRequest = PaymentFailureRecordUpdateRequest(
        augmentedRecords.withBrazeId,
        augmentedRecords.withoutBrazeId,
        brazeResult
      )
      _ <- sfConnector.updateRecords(updateRecordsRequest)
    } yield ()) match {
      case Left(failure) => Log.failure(logger)(failure)
      case Right(_)      => Log.completion(logger)()
    }
  }

  private def augmentRecordsWithBrazeId(idapiConfig: IdapiConfig, logger: LambdaLogger)(
      records: Seq[PaymentFailureRecord]
  ): PartitionedRecords =
    records.foldLeft(PartitionedRecords(Nil, Nil))((acc, record) =>
      IdapiConnector.getBrazeId(idapiConfig, logger)(record.Contact__r.IdentityID__c) match {
        case Left(_) => acc.copy(withoutBrazeId = acc.withoutBrazeId :+ record)
        case Right(brazeId) =>
          acc.copy(withBrazeId = acc.withBrazeId :+ PaymentFailureRecordWithBrazeId(record, brazeId))
      }
    )

  final def main(args: Array[String]): Unit = {
    program(new LambdaLogger {
      def log(message: String): Unit = println(message)
      def log(message: Array[Byte]): Unit = println(message)
    })
  }
}
