package payment_failure_comms.models

import java.time.ZonedDateTime

case class BrazeTrackRequest(events: Seq[CustomEvent])

// Based on https://www.braze.com/docs/api/objects_filters/event_object/
case class CustomEvent(external_id: String, app_id: String, name: String, time: String, properties: EventProperties)

case class EventProperties(product: String, currency: String, amount: Double)

object BrazeTrackRequest {

  private val eventNameMapping = Map(
    "Ready to send entry event" -> "pf_entry",
    "Ready to send recovery event" -> "pf_recovery",
    "Ready to send voluntary cancel event" -> "pf_cancel_voluntary",
    "Ready to send auto cancel event" -> "pf_cancel_auto"
  )

  private[models] def diff(events: Seq[CustomEvent], eventsAlreadyWritten: BrazeUserResponse): Seq[CustomEvent] =
    events.filterNot { event =>
      val eventTime = ZonedDateTime.parse(event.time)
      eventsAlreadyWritten.users.exists { user =>
        user.external_id == event.external_id &&
        user.custom_events.exists(_.name == event.name) &&
        user.custom_events.exists(!_.last.isBefore(eventTime))
      }
    }

  def apply(
      records: Seq[PaymentFailureRecordWithBrazeId],
      zuoraAppId: String,
      eventsAlreadyWritten: BrazeUserResponse
  ): Either[Failure, BrazeTrackRequest] = {

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

    process(Nil, records).map { events =>
      val newEvents = diff(events, eventsAlreadyWritten)
      BrazeTrackRequest(newEvents)
    }
  }

  private def toCustomEvent(
      zuoraAppId: String
  )(record: PaymentFailureRecordWithBrazeId): Either[Failure, CustomEvent] =
    for {
      eventName <- eventNameMapping
        .get(record.record.PF_Comms_Status__c).toRight(
          SalesforceResponseFailure(
            s"Unexpected PF_Comms_Status__c value '${record.record.PF_Comms_Status__c}' in PF record '${record.record.Id}'"
          )
        )
      eventTime <- EventTime(record.record)
    } yield CustomEvent(
      external_id = record.brazeId,
      app_id = zuoraAppId,
      name = eventName,
      time = eventTime,
      properties = EventProperties(
        product = record.record.SF_Subscription__r.Product_Name__c,
        currency = record.record.Currency__c,
        amount = record.record.Invoice_Total_Amount__c
      )
    )
}
