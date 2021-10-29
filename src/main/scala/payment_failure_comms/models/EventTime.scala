package payment_failure_comms.models

import java.time.{LocalDate, OffsetDateTime}
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

object EventTime {

  def apply(record: PaymentFailureRecord): Either[Failure, String] = {
    val failOrDate = eitherFailOrDate(record.Id) _
    if (record.PF_Comms_Status__c == "Ready to process entry")
      failOrDate(record.Initial_Payment_Created_Date__c.map(formatDateTime))
    else
      record.Status__c match {
        case "Recovered" => failOrDate(record.Last_Attempt_Date__c.map(formatDate))
        case "Failed" | "Already Cancelled" =>
          failOrDate(record.SF_Subscription__r.Cancellation_Request_Date__c.map(formatDateTime))
        case "Auto-Cancel Failure" => Right(formatDate(record.Cut_Off_Date__c))
        case other => Left(SalesforceResponseFailure(s"Unexpected status value '$other' in PF record ${record.Id}"))
      }
  }

  def formatDateTime(odt: OffsetDateTime): String =
    odt.withOffsetSameInstant(UTC).format(ISO_OFFSET_DATE_TIME)

  def formatDate(d: LocalDate): String =
    d.atStartOfDay(UTC).format(ISO_OFFSET_DATE_TIME)

  def eitherFailOrDate(recordId: String)(date: Option[String]): Either[Failure, String] = date.toRight(
    SalesforceResponseFailure(s"Missing event-time field in PF record $recordId")
  )
}
