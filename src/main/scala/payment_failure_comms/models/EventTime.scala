package payment_failure_comms.models

import java.time.{LocalDate, OffsetDateTime}
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

object EventTime {

  def apply(record: PaymentFailureRecord): Either[Failure, String] = {
    val failOrDate = eitherFailOrDate(record.Id) _
    record.PF_Comms_Status__c match {
      case "Ready to send entry event"    => failOrDate(record.Initial_Payment_Created_Date__c.map(formatDateTime))
      case "Ready to send recovery event" => failOrDate(record.Recovery_Date__c.map(formatDateTime))
      case "Ready to send voluntary cancel event" =>
        failOrDate(record.SF_Subscription__r.Cancellation_Request_Date__c.map(formatDateTime))
      case "Ready to send auto cancel event" => Right(formatDate(record.Cut_Off_Date__c))
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
