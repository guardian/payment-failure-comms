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
      augmentedRecords = augmentRecordsWithBrazeId(findBrazeId(config.idapi, logger))(records)

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
      case Left(failure) =>
        Log.failure(logger)(failure)
        throw new RuntimeException(failure.details)
      case Right(_) => Log.completion(logger)
    }
  }

  /*
   * We find the Braze ID by looking it up in the IDAPI if the contact record has an Identity ID.
   * Otherwise, if the contact doesn't have an Identity ID, we fall back to using the contact ID as Braze ID.
   * This is an established pattern so there should be matching accounts in Braze with these IDs.
   */
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

  private def augmentRecordsWithBrazeId(findBrazeId: PaymentFailureRecord => Option[String])(
      records: Seq[PaymentFailureRecord]
  ): PartitionedRecords =
    records.foldLeft(PartitionedRecords(Nil, Nil))((acc, record) =>
      findBrazeId(record) match {
        case None => acc.copy(withoutBrazeId = acc.withoutBrazeId :+ record)
        case Some(brazeId) =>
          acc.copy(withBrazeId = acc.withBrazeId :+ PaymentFailureRecordWithBrazeId(record, brazeId))
      }
    )

  final def main(args: Array[String]): Unit =
    program(ConsoleLogger())
}
