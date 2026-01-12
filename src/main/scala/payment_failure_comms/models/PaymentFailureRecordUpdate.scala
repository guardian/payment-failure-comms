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

  val eventStageMapping: Map[String, String] = Map(
    "Ready to send entry event" -> "Entry",
    "Ready to send recovery event" -> "Exit",
    "Ready to send voluntary cancel event" -> "Exit",
    "Ready to send auto cancel event" -> "Exit"
  )

  def apply(brazeResult: Either[Failure, Unit])(record: PaymentFailureRecord): PaymentFailureRecordUpdate =
    brazeResult match {
      case Right(_) => successfulUpdate(record)
      case Left(_)  => failedUpdate(record)
    }

  def successfulUpdate(record: PaymentFailureRecord): PaymentFailureRecordUpdate = {
    PaymentFailureRecordUpdate(
      Id = record.Id,
      PF_Comms_Last_Stage_Processed__c = eventStageMapping.get(record.PF_Comms_Status__c),
      PF_Comms_Number_of_Attempts__c = 0
    )
  }

  def failedUpdate(record: PaymentFailureRecord): PaymentFailureRecordUpdate = {
    PaymentFailureRecordUpdate(
      Id = record.Id,
      PF_Comms_Last_Stage_Processed__c = record.PF_Comms_Last_Stage_Processed__c,
      PF_Comms_Number_of_Attempts__c = record.PF_Comms_Number_of_Attempts__c.getOrElse(0) + 1
    )
  }
}

object PaymentFailureRecordUpdateRequest {
  def apply(
      recordsWithBrazeId: Seq[PaymentFailureRecordWithBrazeId],
      recordsWithoutBrazeId: Seq[PaymentFailureRecord],
      brazeResult: Either[Failure, Unit]
  ): PaymentFailureRecordUpdateRequest =
    PaymentFailureRecordUpdateRequest(
      recordsWithBrazeId.map(record => PaymentFailureRecordUpdate(brazeResult)(record.record)) ++
        recordsWithoutBrazeId.map(PaymentFailureRecordUpdate.failedUpdate)
    )
}
