package payment_failure_comms.models

case class BrazeTrackRequest(events: Seq[CustomEvent])

// Based on https://www.braze.com/docs/api/objects_filters/event_object/
case class CustomEvent(external_id: String, app_id: String, name: String, time: String, properties: EventProperties)

case class EventProperties(product: String, currency: String, amount: Double)

object BrazeTrackRequest {

  val eventNameMapping = Map(
    "In Progress" -> "payment_failure",
    "Recovered" -> "payment_recovery",
    "Auto-Cancel Failure" -> "payment_failure_cancelation",
    "Already Cancelled" -> "subscription_cancelation"
  )

  def apply(records: Seq[PaymentFailureRecordWithBrazeId], zuoraAppId: String): Either[Failure, BrazeTrackRequest] = {

    val customEvent = toCustomEvent(zuoraAppId) _

    def process(
        soFar: Seq[CustomEvent],
        toGo: Seq[PaymentFailureRecordWithBrazeId]
    ): Either[Failure, Seq[CustomEvent]] =
      toGo match {
        case currRecord :: restOfRecords =>
          customEvent(currRecord).flatMap(event => process(soFar :+ event, restOfRecords))
        case _ => Right(soFar)
      }

    process(Nil, records).map(BrazeTrackRequest.apply)
  }

  private def toCustomEvent(
      zuoraAppId: String
  )(record: PaymentFailureRecordWithBrazeId): Either[Failure, CustomEvent] = {
    // TODO: Consider handling case when record.record.Status__c doesn't exist in eventNameMapping differently,
    // despite it not being a realistic possibility (Possible contents of field need to be altered in Salesforce for that to happen)
    val eventName = eventNameMapping.getOrElse(record.record.Status__c, "")
    val eventProperties = EventProperties(
      product = record.record.SF_Subscription__r.Product_Name__c,
      currency = record.record.Currency__c,
      amount = record.record.Invoice_Total_Amount__c
    )

    EventTime(record.record).map { eventTime =>
      CustomEvent(
        external_id = record.brazeId,
        app_id = zuoraAppId,
        name = eventName,
        time = eventTime,
        properties = eventProperties
      )
    }
  }
}
