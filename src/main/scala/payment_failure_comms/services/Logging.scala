package payment_failure_comms.services

import com.amazonaws.services.lambda.runtime.LambdaLogger
import payment_failure_comms.Log
import payment_failure_comms.Log.Service
import payment_failure_comms.models.Failure
import zio.{UIO, ZIO}

trait Logging {
  def logFailure(failure: Failure): UIO[Unit]
  def logRequest(
      service: Service,
      description: Option[String] = None,
      url: String,
      method: String,
      query: Option[String] = None,
      body: Option[String] = None
  ): UIO[Unit]
}

case class LoggingLive(lambdaLogger: LambdaLogger) extends Logging {

  def logFailure(failure: Failure): UIO[Unit] =
    ZIO.succeed(Log.failure(lambdaLogger)(failure))

  def logRequest(
      service: Service,
      description: Option[String],
      url: String,
      method: String,
      query: Option[String],
      body: Option[String]
  ): UIO[Unit] =
    ZIO.succeed(Log.request(lambdaLogger)(service, description, url, method, query, body))
}
