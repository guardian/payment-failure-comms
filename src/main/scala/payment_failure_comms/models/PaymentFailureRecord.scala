package payment_failure_comms.models

case class PaymentFailureRecord(
    Id: String,
    Contact__r: SFContact,
    Status__c: String,
    PF_Comms_Number_of_Attempts__c: Option[Int] = None
)

case class SFContact(IdentityID__c: String)

case class SFPaymentFailureRecordWrapper(totalSize: Int, done: Boolean, records: Seq[PaymentFailureRecord])
