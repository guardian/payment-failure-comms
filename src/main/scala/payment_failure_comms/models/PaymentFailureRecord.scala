package payment_failure_comms.models

// TBD
case class PaymentFailureRecord()

case class SFPaymentFailureRecordWrapper(totalSize: Int, done: Boolean, records: Seq[PaymentFailureRecord])
