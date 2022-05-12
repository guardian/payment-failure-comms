package payment_failure_comms.service

import payment_failure_comms.models.{Failure, PaymentFailureRecord}
import zio._

trait Idapi {
  def fetchBrazeId(record: PaymentFailureRecord): IO[Failure, String]
}

object Idapi {
  def fetchBrazeId(record: PaymentFailureRecord): ZIO[Idapi, Failure, String] =
    ZIO.serviceWithZIO(_.fetchBrazeId(record))
}
