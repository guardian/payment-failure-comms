package payment_failure_comms.models

import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime

case class BrazeTrackRequest(events: Seq[CustomEvent])

// Based on https://www.braze.com/docs/api/objects_filters/event_object/
case class CustomEvent(external_id: String, app_id: String, name: String, time: String, properties: EventProperties)

case class EventProperties(currency: String, amount: Double)

object BrazeTrackRequest {

  val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss:SSSZ")
  val eventNameMapping = Map(
    "In Progress" -> "payment_failure",
    "Recovered" -> "payment_recovery",
    "Auto-Cancel Failure" -> "payment_failure_cancelation",
    "Already Cancelled" -> "subscription_cancelation"
  )

  def apply(records: Seq[PaymentFailureRecordWithBrazeId], zuoraAppId: String): BrazeTrackRequest = {

    val events = records
      .map(record => {
        // TODO: Consider handling case when record.record.Status__c doesn't exist in eventNameMapping differently,
        // despite it not being a realistic possibility (Possible contents of field need to be altered in Salesforce for that to happen)
        val eventName = eventNameMapping.getOrElse(record.record.Status__c, "")
        // TODO: Determine eventTime from record. Entry and exit events will fetch date from different fields
        val eventTime = dateTimeFormatter.format(ZonedDateTime.now)
        val eventProperties = EventProperties(
          currency = record.record.Currency__c,
          amount = record.record.Invoice_Total_Amount__c
        )

        CustomEvent(
          external_id = record.brazeId,
          app_id = zuoraAppId,
          name = eventName,
          time = eventTime,
          properties = eventProperties
        )
      })

    BrazeTrackRequest(events)
  }

}
