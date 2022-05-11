package payment_failure_comms

import com.amazonaws.services.lambda.runtime.LambdaLogger
import io.circe.generic.auto._
import io.circe.syntax._
import okhttp3._
import payment_failure_comms.models._

// Required to encode BrazeTrackRequest
import payment_failure_comms.models.EncodeCustomAttribute._

import scala.util.Try

object BrazeConnector {

  private val JSON: MediaType = MediaType.get("application/json; charset=utf-8")

  private def logRequest(logger: LambdaLogger, body: String, request: Request): Unit =
    Log.request(logger)(
      service = Log.Service.Braze,
      url = request.url().toString,
      method = request.method(),
      body = Some(body)
    )

  private def logResponse(logger: LambdaLogger, response: Response, body: String): Unit =
    Log.response(logger)(
      service = Log.Service.Braze,
      url = response.request().url().toString,
      method = response.request().method(),
      responseCode = response.code(),
      body = Some(body)
    )

  def sendCustomEvents(brazeConfig: BrazeConfig, logger: LambdaLogger)(
      payload: BrazeTrackRequest
  ): Either[Failure, Unit] = {
    if (payload.events.isEmpty)
      Right(())
    else
      handleRequestResult(logger)(
        responseToPostRequest(logger)(
          url = s"https://${brazeConfig.instanceUrl}/users/track",
          bearerToken = brazeConfig.bearerToken,
          body = payload.asJson.toString
        )
      )
  }

  def responseToPostRequest(
      logger: LambdaLogger
  )(url: String, bearerToken: String, body: String): Either[Throwable, Response] = {
    val request: Request = new Request.Builder()
      .header("Authorization", s"Bearer $bearerToken")
      .url(url)
      .post(RequestBody.create(body, JSON))
      .build()
    logRequest(logger, body, request)
    Try(
      HttpClient().newCall(request).execute()
    ).toEither
  }

  def handleRequestResult(logger: LambdaLogger)(result: Either[Throwable, Response]): Either[Failure, Unit] = {
    result
      .left.map(i => BrazeRequestFailure(s"Attempt to contact Braze failed with error: ${i.toString}"))
      .flatMap(response => {
        val body = response.body().string()
        logResponse(logger, response, body)
        if (response.isSuccessful) {
          Right(())
        } else {
          Left(BrazeResponseFailure(s"The request to Braze was unsuccessful: ${response.code} - $body"))
        }
      })
  }
}
