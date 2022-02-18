package payment_failure_comms.services

import payment_failure_comms.Log.Service
import payment_failure_comms.models.Failure
import zio._

trait Logging {

  def logFailure(failure: Failure): UIO[Unit]

  def logRequest(
      service: Service,
      description: Option[String] = None,
      url: String,
      method: String,
      query: Option[String] = None,
      body: Option[String] = None,
  ): UIO[Unit]
}

object Logging {

  def logFailure(failure: Failure): URIO[Logging, Unit] = URIO.serviceWith(_.logFailure(failure))

  def logRequest(
      service: Service,
      description: Option[String] = None,
      url: String,
      method: String,
      query: Option[String] = None,
      body: Option[String] = None,
  ): URIO[Logging, Unit] = URIO.serviceWith(_.logRequest(service, description, url, method, query, body))
}
