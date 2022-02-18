package payment_failure_comms.services

import com.amazonaws.services.lambda.runtime.LambdaLogger
import payment_failure_comms.Log
import payment_failure_comms.Log.Service
import payment_failure_comms.models.Failure
import zio._

object LoggingLive {
  def layer(lambdaLogger: LambdaLogger): ZLayer[Any, Nothing, Logging] =
    ZLayer.fromZIO(
      ZIO.succeed(
        new Logging {

          override def logFailure(failure: Failure): UIO[Unit] = ZIO.succeed(Log.failure(lambdaLogger)(failure))

          override def logRequest(
              service: Service,
              description: Option[String],
              url: String,
              method: String,
              query: Option[String],
              body: Option[String],
          ): UIO[Unit] = ZIO.succeed(Log.request(lambdaLogger)(service, description, url, method, query, body))
        },
      ),
    )
}
