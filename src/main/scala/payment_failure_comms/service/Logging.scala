package payment_failure_comms.service

import com.amazonaws.services.lambda.runtime.LambdaLogger
import payment_failure_comms.Log
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
      body: Option[String] = None
  ): UIO[Unit]

  def logResponse(
      service: Service,
      description: Option[String] = None,
      url: String,
      method: String,
      query: Option[String] = None,
      responseCode: Int,
      body: Option[String] = None
  ): UIO[Unit]
}

object Logging {

  def logFailure(failure: Failure): URIO[Logging, Unit] = ZIO.serviceWithZIO(_.logFailure(failure))

  def logRequest(
      service: Service,
      description: Option[String] = None,
      url: String,
      method: String,
      query: Option[String] = None,
      body: Option[String] = None
  ): URIO[Logging, Unit] =
    ZIO.serviceWithZIO(_.logRequest(service, description, url, method, query, body))

  def logResponse(
      service: Service,
      description: Option[String] = None,
      url: String,
      method: String,
      query: Option[String] = None,
      responseCode: Int,
      body: Option[String] = None
  ): URIO[Logging, Unit] =
    ZIO.serviceWithZIO(_.logResponse(service, description, url, method, query, responseCode, body))
}

object LoggingLive {
  def layer(lambdaLogger: LambdaLogger): ULayer[Logging] =
    ZLayer.succeed(
      new Logging {

        override def logFailure(failure: Failure): UIO[Unit] = ZIO.succeed(Log.failure(lambdaLogger)(failure))

        override def logRequest(
            service: Service,
            description: Option[String],
            url: String,
            method: String,
            query: Option[String],
            body: Option[String]
        ): UIO[Unit] =
          ZIO.succeed(Log.request(lambdaLogger)(service, description, url, method, query, body))

        override def logResponse(
            service: Service,
            description: Option[String],
            url: String,
            method: String,
            query: Option[String],
            responseCode: Int,
            body: Option[String]
        ): UIO[Unit] =
          ZIO.succeed(Log.response(lambdaLogger)(service, description, url, method, query, responseCode, body))
      }
    )
}
