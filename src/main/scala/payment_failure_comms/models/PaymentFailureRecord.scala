package payment_failure_comms.models

import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.{LocalDate, OffsetDateTime, ZoneOffset}

case class PaymentFailureRecord(
    Id: String,
    Contact__r: SFContact,
    SF_Subscription__r: SFSubscription,
    Status__c: String,
    PF_Comms_Last_Stage_Processed__c: Option[String] = None,
    PF_Comms_Number_of_Attempts__c: Option[Int] = Some(0),
    Currency__c: String,
    Invoice_Total_Amount__c: Double,
    Initial_Payment_Created_Date__c: Option[OffsetDateTime],
    Last_Attempt_Date__c: Option[LocalDate]
) {
  val eventTime: Either[Failure, String] = {

    def formatDateTime(odt: Option[OffsetDateTime]) =
      optDateTimeToEither(odt.map(_.withOffsetSameInstant(UTC).format(ISO_OFFSET_DATE_TIME)))

    def formatDate(odt: Option[LocalDate]) =
      optDateTimeToEither(odt.map(_.atStartOfDay(UTC).format(ISO_OFFSET_DATE_TIME)))

    def optDateTimeToEither(s: Option[String]) = s.toRight(
      SalesforceResponseFailure(s"Missing event-time field in PF record $Id")
    )

    (Status__c match {
      case "Error-default" => formatDateTime(Initial_Payment_Created_Date__c)
      case "Recovered"     => formatDate(Last_Attempt_Date__c)
      case "Failed" | "Already Cancelled" | "Auto-Cancel Failure" =>
        formatDateTime(SF_Subscription__r.Cancellation_Request_Date__c)
      case other => Left(SalesforceResponseFailure(s"Unexpected status value '$Status__c' in PF record $Id"))
    })
  }
}

case class SFContact(IdentityID__c: String)

case class SFSubscription(
    Product_Name__c: String,
    Cancellation_Request_Date__c: Option[OffsetDateTime]
)

case class SFPaymentFailureRecordWrapper(totalSize: Int, done: Boolean, records: Seq[PaymentFailureRecord])
