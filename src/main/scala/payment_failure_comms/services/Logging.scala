package payment_failure_comms.services

import com.amazonaws.services.lambda.runtime.LambdaLogger
import payment_failure_comms.Log
import payment_failure_comms.Log.Service
import payment_failure_comms.models.Failure
import zio.{Has, UIO, ULayer, URIO, ZIO}

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

object Logging {

  def logFailure(failure: Failure): URIO[Has[Logging], Unit] = URIO.serviceWith(_.logFailure(failure))

  def logRequest(
      service: Service,
      description: Option[String] = None,
      url: String,
      method: String,
      query: Option[String] = None,
      body: Option[String] = None
  ): URIO[Has[Logging], Unit] = URIO.serviceWith(_.logRequest(service, description, url, method, query, body))
}

object LoggingLive {

  def effect(lambdaLogger: LambdaLogger): UIO[Logging] = UIO.succeed(new Logging {
    def logFailure(failure: Failure): UIO[Unit] = ZIO.succeed(Log.failure(lambdaLogger)(failure))
    def logRequest(
        service: Service,
        description: Option[String],
        url: String,
        method: String,
        query: Option[String],
        body: Option[String]
    ): UIO[Unit] =
      ZIO.succeed(Log.request(lambdaLogger)(service, description, url, method, query, body))
  })

  def layer(lambdaLogger: LambdaLogger): ULayer[Has[Logging]] = effect(lambdaLogger).toLayer
}
