package payment_failure_comms.models

import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime

case class BrazeTrackRequest(events: Seq[CustomEvent])

// Based on https://www.braze.com/docs/api/objects_filters/event_object/
case class CustomEvent(external_id: String, app_id: String, name: String, time: String, properties: EventProperties)

case class EventProperties(currency: String, amount: Double)

object BrazeTrackRequest {

  val eventNameMapping = Map(
    "In Progress" -> "payment_failure",
    "Recovered" -> "payment_recovery",
    "Auto-Cancel Failure" -> "payment_failure_cancelation",
    "Already Cancelled" -> "subscription_cancelation"
  )

  def apply(records: Seq[PaymentFailureRecordWithBrazeId], zuoraAppId: String): BrazeTrackRequest = {

    val events = records
      .filter(_.brazeId.isRight)
      .map(record => {
        // TODO: Throw error if record.record.Status__c doesn't exist in eventNameMapping
        val eventName = eventNameMapping.getOrElse(record.record.Status__c, "")
        // TODO: Determine eventTime from record. Entry and exit events will fetch date from different fields
        val eventTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss:SSSZ").format(ZonedDateTime.now)
        // TODO: Determine eventProperties from record
        val eventProperties = EventProperties("EUR", 0.00)

        CustomEvent(
          external_id = record.brazeId.getOrElse(""), // The OrElse never happens because it's filtered out above
          app_id = zuoraAppId,
          name = eventName,
          time = eventTime,
          properties = eventProperties
        )
      })

    BrazeTrackRequest(events)
  }

}
