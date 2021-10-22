package payment_failure_comms.models

import java.time.{LocalDate, OffsetDateTime}
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

object EventTime {

  def apply(record: PaymentFailureRecord): Either[Failure, String] =
    record.Status__c match {
      case "Error-default" => eitherFailOrDate(record.Id, record.Initial_Payment_Created_Date__c.map(formatDateTime))
      case "Recovered"     => eitherFailOrDate(record.Id, record.Last_Attempt_Date__c.map(formatDate))
      case "Failed" | "Already Cancelled" | "Auto-Cancel Failure" =>
        eitherFailOrDate(record.Id, record.SF_Subscription__r.Cancellation_Request_Date__c.map(formatDateTime))
      case other => Left(SalesforceResponseFailure(s"Unexpected status value '$other' in PF record ${record.Id}"))
    }

  def formatDateTime(odt: OffsetDateTime): String =
    odt.withOffsetSameInstant(UTC).format(ISO_OFFSET_DATE_TIME)

  def formatDate(d: LocalDate): String =
    d.atStartOfDay(UTC).format(ISO_OFFSET_DATE_TIME)

  def eitherFailOrDate(recordId: String, date: Option[String]): Either[Failure, String] = date.toRight(
    SalesforceResponseFailure(s"Missing event-time field in PF record $recordId")
  )
}
