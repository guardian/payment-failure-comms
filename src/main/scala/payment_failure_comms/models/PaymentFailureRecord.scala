package payment_failure_comms.models

// TODO: Add actual fields to this case class
case class PaymentFailureRecord()

case class SFPaymentFailureRecordWrapper(totalSize: Int, done: Boolean, records: Seq[PaymentFailureRecord])
