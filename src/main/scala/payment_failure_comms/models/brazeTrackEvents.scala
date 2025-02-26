package payment_failure_comms.models

import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._

import java.time.LocalDate

case class BrazeTrackRequest(attributes: Seq[CustomAttribute], events: Seq[CustomEvent])

case class CustomEventWithAttributes(attributes: Seq[CustomAttribute], event: CustomEvent)

// Based on https://www.braze.com/docs/api/objects_filters/event_object/
case class CustomEvent(external_id: String, app_id: String, name: String, time: String, properties: EventProperties)

sealed trait CustomAttribute {
  def external_id: String
}

/* NOTE: if adding additional custom attributes, consider the limit when sending the attributes to Braze.
There is a current limit of 75 attributes per API call to Braze */
// https://www.braze.com/docs/api/endpoints/user_data/post_user_track/#rate-limit
case class PaymentFailureTypeAttr(external_id: String, payment_failure_type: Option[String] = Some(""))
    extends CustomAttribute
case class ResponseCodeAttr(external_id: String, gateway_response_code: Option[String] = Some(""))
    extends CustomAttribute
case class ResponseMessageAttr(external_id: String, gateway_response_message: Option[String] = Some(""))
    extends CustomAttribute
case class RecoveryDateAttr(external_id: String, recovery_date: Option[LocalDate]) extends CustomAttribute
case class SubscriptionIdAttr(external_id: String, subscription_id: Option[String] = Some("")) extends CustomAttribute
case class ProductNameAttr(external_id: String, product_name: String) extends CustomAttribute
case class InvoiceCreatedDateAttr(external_id: String, invoice_created_date: Option[LocalDate]) extends CustomAttribute
case class BillToCountryAttr(external_id: String, bill_to_country: Option[String] = Some("")) extends CustomAttribute

object EncodeCustomAttribute {
  implicit val encode: Encoder[CustomAttribute] = Encoder.instance {
    case attr @ PaymentFailureTypeAttr(_, _) => attr.asJson
    case attr @ ResponseCodeAttr(_, _)       => attr.asJson
    case attr @ ResponseMessageAttr(_, _)    => attr.asJson
    case attr @ RecoveryDateAttr(_, _)    => attr.asJson
    case attr @ SubscriptionIdAttr(_, _)     => attr.asJson
    case attr @ ProductNameAttr(_, _)        => attr.asJson
    case attr @ InvoiceCreatedDateAttr(_, _) => attr.asJson
    case attr @ BillToCountryAttr(_, _)      => attr.asJson
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

  def apply(
      records: Seq[PaymentFailureRecordWithBrazeId],
      zuoraAppId: String
  ): Either[Failure, Seq[BrazeTrackRequest]] = {

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

    process(Nil, records).map { eventsAndAttributes =>
      // Send events in groups of 9 to Braze so we do not hit the attribute limit of 75 per API call
      // https://www.braze.com/docs/api/endpoints/user_data/post_user_track/#rate-limit
      eventsAndAttributes
        .grouped(9).map(group => BrazeTrackRequest(group.flatMap(_.attributes), group.map(_.event))).toSeq
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
      import record.record.{Invoice_Created_Date__c, Recovery_Date__c, SF_Subscription__r}

      CustomEventWithAttributes(
        Seq(
          PaymentFailureTypeAttr(record.brazeId, record.record.Payment_Failure_Type__c),
          ResponseCodeAttr(record.brazeId, record.record.STG_Initial_Gateway_Response_Code__c),
          ResponseMessageAttr(record.brazeId, record.record.STG_Initial_Gateway_Response__c),
          RecoveryDateAttr(record.brazeId, Recovery_Date__c),
          SubscriptionIdAttr(record.brazeId, SF_Subscription__r.Zuora_Subscription_Name__c),
          ProductNameAttr(record.brazeId, SF_Subscription__r.Product_Name__c.getOrElse("")),
          InvoiceCreatedDateAttr(record.brazeId, Invoice_Created_Date__c),
          BillToCountryAttr(record.brazeId, record.record.Billing_Account__r.Zuora__BillToCountry__c)
        ),
        CustomEvent(
          external_id = record.brazeId,
          app_id = zuoraAppId,
          name = eventName,
          time = eventTime,
          properties = EventProperties(
            product = SF_Subscription__r.Product_Name__c.getOrElse(""),
            currency = record.record.Currency__c,
            amount = record.record.Invoice_Total_Amount__c
          )
        )
      )
    }
}
