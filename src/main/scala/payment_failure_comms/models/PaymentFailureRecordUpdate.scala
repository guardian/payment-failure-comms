package payment_failure_comms.models

case class PaymentFailureRecordUpdate(
    Id: String,
    PF_Comms_Last_Stage_Processed__c: Option[String] = None,
    PF_Comms_Number_of_Attempts__c: Int,
    attributes: Attributes = Attributes(`type` = "Payment_Failure__c")
)

case class Attributes(`type`: String)

case class PaymentFailureRecordUpdateRequest(records: Seq[PaymentFailureRecordUpdate]) {
  def allOrNone = false
}

object PaymentFailureRecordUpdate {

  val eventStageMapping = Map(
    "In Progress" -> "Entry",
    "Recovered" -> "Exit",
    "Auto-Cancel Failure" -> "Exit",
    "Already Cancelled" -> "Exit"
  )

  def apply(
      record: PaymentFailureRecordWithBrazeId,
      brazeResult: Either[Failure, Unit]
  ): PaymentFailureRecordUpdate = {
    brazeResult match {
      case Right(_) => successfulUpdate(record)
      case Left(_)  => failedUpdate(record)
    }
  }

  def successfulUpdate(augmentedRecord: PaymentFailureRecordWithBrazeId) = {
    PaymentFailureRecordUpdate(
      augmentedRecord.record.Id,
      eventStageMapping.get(augmentedRecord.record.Status__c),
      0
    )
  }

  def failedUpdate(augmentedRecord: PaymentFailureRecordWithBrazeId) = {
    PaymentFailureRecordUpdate(
      augmentedRecord.record.Id,
      augmentedRecord.record.PF_Comms_Last_Stage_Processed__c,
      augmentedRecord.record.PF_Comms_Number_of_Attempts__c.getOrElse(0) + 1
    )
  }
}

object PaymentFailureRecordUpdateRequest {
  def apply(
      augmentedRecords: Seq[PaymentFailureRecordWithBrazeId],
      brazeResult: Either[Failure, Unit]
  ): PaymentFailureRecordUpdateRequest = {
    PaymentFailureRecordUpdateRequest(
      augmentedRecords
        .map(PaymentFailureRecordUpdate(_, brazeResult))
    )
  }
}
