package payment_failure_comms.models
import scala.collection.mutable.Map
import java.time.{LocalDate, OffsetDateTime, ZonedDateTime}
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._
import payment_failure_comms.models

case class BrazeTrackRequest(attributes: Seq[CustomAttribute], events: Seq[CustomEvent])

case class CustomEventWithAttributes(attributes: Seq[CustomAttribute], event: CustomEvent)

// Based on https://www.braze.com/docs/api/objects_filters/event_object/
case class CustomEvent(external_id: String, app_id: String, name: String, time: String, properties: EventProperties)

sealed trait CustomAttribute {
  def external_id: String
}

case class ResponseCodeAttr(external_id: String, gateway_response_code: String) extends CustomAttribute
case class ResponseMessageAttr(external_id: String, gateway_response_message: String) extends CustomAttribute
case class LastAttemptDateAttr(external_id: String, last_attempt_date: Option[LocalDate]) extends CustomAttribute
case class SubscriptionIdAttr(external_id: String, subscription_id: String) extends CustomAttribute
case class ProductNameAttr(external_id: String, product_name: String) extends CustomAttribute
case class InvoiceCreatedDateAttr(external_id: String, invoice_created_date: Option[LocalDate]) extends CustomAttribute
case class BillToCountryAttr(external_id: String, bill_to_country: String) extends CustomAttribute

object EncodeCustomAttribute {
  implicit val encode: Encoder[CustomAttribute] = Encoder.instance {
    case c1 @ ResponseCodeAttr(_, _)       => c1.asJson
    case c2 @ ResponseMessageAttr(_, _)    => c2.asJson
    case c3 @ LastAttemptDateAttr(_, _)    => c3.asJson
    case c4 @ SubscriptionIdAttr(_, _)     => c4.asJson
    case c5 @ ProductNameAttr(_, _)        => c5.asJson
    case c6 @ InvoiceCreatedDateAttr(_, _) => c6.asJson
    case c7 @ BillToCountryAttr(_, _)      => c7.asJson
  }
}

case class EventProperties(
    product: String,
    currency: String,
    amount: Double
)

object BrazeTrackRequest {

  private val eventNameMapping = Map(
    "Ready to send entry event" -> "pf_entry",
    "Ready to send recovery event" -> "pf_recovery",
    "Ready to send voluntary cancel event" -> "pf_cancel_voluntary",
    "Ready to send auto cancel event" -> "pf_cancel_auto"
  )

  private def timeOf(event: CustomEvent) = ZonedDateTime.parse(event.time)

  private def hasEventWithSameNameAndAtSameTimeOrLater(
      existingUserEvents: Option[Seq[UserCustomEvent]]
  )(record: CustomEventWithAttributes) =
    existingUserEvents.exists(
      _.exists(existingEvent =>
        existingEvent.name == record.event.name && !existingEvent.last.isBefore(timeOf(record.event))
      )
    )

  private def isAlreadyInBraze(eventsAlreadyWritten: BrazeUserResponse)(record: CustomEventWithAttributes) =
    eventsAlreadyWritten.users.exists { user =>
      user.external_id == record.event.external_id &&
      hasEventWithSameNameAndAtSameTimeOrLater(user.custom_events)(record)
    }

  private[models] def diff(
      events: Seq[CustomEventWithAttributes],
      eventsAlreadyWritten: BrazeUserResponse
  ): Seq[CustomEventWithAttributes] =
    events.filterNot(isAlreadyInBraze(eventsAlreadyWritten))

  def apply(
      records: Seq[PaymentFailureRecordWithBrazeId],
      zuoraAppId: String,
      eventsAlreadyWritten: BrazeUserResponse
  ): Either[Failure, BrazeTrackRequest] = {

    val processRecordFunc = processRecord(zuoraAppId) _

    def process(
        soFar: Seq[CustomEventWithAttributes],
        toGo: Seq[PaymentFailureRecordWithBrazeId]
    ): Either[Failure, Seq[CustomEventWithAttributes]] =
      toGo match {
        case currRecord :: restOfRecords =>
          processRecordFunc(currRecord).flatMap(event => process(soFar :+ event, restOfRecords))
        case _ => Right(soFar)
      }

    process(Nil, records).map { records =>
      val newRecords = diff(records, eventsAlreadyWritten)

      BrazeTrackRequest(
        attributes = newRecords.flatMap(rec => rec.attributes),
        events = newRecords.map(rec => rec.event)
      )
    }
  }

  private def processRecord(
      zuoraAppId: String
  )(record: PaymentFailureRecordWithBrazeId): Either[Failure, CustomEventWithAttributes] =
    for {
      eventName <- eventNameMapping
        .get(record.record.PF_Comms_Status__c).toRight(
          SalesforceResponseFailure(
            s"Unexpected PF_Comms_Status__c value '${record.record.PF_Comms_Status__c}' in PF record '${record.record.Id}'"
          )
        )
      eventTime <- EventTime(record.record)
    } yield {
      import record.record.{Initial_Payment__r, SF_Subscription__r, Last_Attempt_Date__c, Invoice_Created_Date__c}

      CustomEventWithAttributes(
        Seq(
          ResponseCodeAttr(record.brazeId, Initial_Payment__r.Zuora__GatewayResponseCode__c),
          ResponseMessageAttr(record.brazeId, Initial_Payment__r.Zuora__GatewayResponse__c),
          LastAttemptDateAttr(record.brazeId, Last_Attempt_Date__c),
          SubscriptionIdAttr(record.brazeId, SF_Subscription__r.Zuora_Subscription_Name__c),
          ProductNameAttr(record.brazeId, SF_Subscription__r.Product_Name__c),
          InvoiceCreatedDateAttr(record.brazeId, Invoice_Created_Date__c),
          BillToCountryAttr(record.brazeId, record.record.Billing_Account__r.Zuora__BillToCountry__c)
        ),
        CustomEvent(
          external_id = record.brazeId,
          app_id = zuoraAppId,
          name = eventName,
          time = eventTime,
          properties = EventProperties(
            product = SF_Subscription__r.Product_Name__c,
            currency = record.record.Currency__c,
            amount = record.record.Invoice_Total_Amount__c
          )
        )
      )
    }
}
