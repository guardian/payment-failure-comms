package payment_failure_comms

import payment_failure_comms.models.{
  BrazeTrackRequest,
  Config,
  Failure,
  IdapiConfig,
  PaymentFailureRecord,
  PaymentFailureRecordUpdateRequest,
  PaymentFailureRecordWithBrazeId
}

object Handler {

  def handleRequest(): Unit = {
    (for {
      config <- Config()
      sfConnector <- SalesforceConnector(config.salesforce)

      records <- sfConnector.getRecordsToProcess()
      recordsWithBrazeId = augmentRecords(config.idapi, records)

      brazeRequest = BrazeTrackRequest(recordsWithBrazeId, config.braze.zuoraAppId)
      brazeResult = BrazeConnector.sendCustomEvent(config.braze, brazeRequest)

      updateRecordsRequest = PaymentFailureRecordUpdateRequest(recordsWithBrazeId, brazeResult)
      updateRecordsResult <- sfConnector.updateRecords(updateRecordsRequest)

      // TODO: Process updateRecordsResult for eventual failures

    } yield ()) match {
      case Left(failure) => println(failure)
      case Right(_)      => println("I totally just ran.")
    }
  }

  def augmentRecords(
      idapiConfig: IdapiConfig,
      records: Seq[PaymentFailureRecord]
  ): PartitionedRecords =
    records.foldLeft(PartitionedRecords(Nil, Nil))((acc, record) =>
      IdapiConnector.getBrazeId(idapiConfig, record.Contact__r.IdentityID__c) match {
        case Left(_) => acc.copy(withoutBrazeId = acc.withoutBrazeId :+ record)
        case Right(brazeId) =>
          acc.copy(withBrazeId = acc.withBrazeId :+ PaymentFailureRecordWithBrazeId(record, brazeId))
      }
    )
}
