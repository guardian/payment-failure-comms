package payment_failure_comms.models

case class PaymentFailureRecordWithBrazeId(record: PaymentFailureRecord, brazeId: Either[Failure, String])
