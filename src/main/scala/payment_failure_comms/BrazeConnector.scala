package payment_failure_comms

import com.amazonaws.services.lambda.runtime.LambdaLogger
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import okhttp3._
import payment_failure_comms.models._

import scala.util.Try

object BrazeConnector {

  private val JSON: MediaType = MediaType.get("application/json; charset=utf-8")

  def fetchCustomEvents(brazeConfig: BrazeConfig, logger: LambdaLogger)(
      payload: BrazeUserRequest
  ): Either[Failure, BrazeUserResponse] = {
    if (payload.external_ids.isEmpty)
      Right(BrazeUserResponse(Nil))
    else
      responseToPostRequest(logger)(
        url = s"https://${brazeConfig.instanceUrl}/users/export/ids",
        bearerToken = brazeConfig.bearerToken,
        body = payload.asJson.toString
      )
        .left.map(ex => BrazeRequestFailure(s"Attempt to contact Braze failed with error: ${ex.toString}"))
        .flatMap(response => {

          val body = response.body().string()

          Log.response(logger)(
            service = Log.Service.Braze,
            url = response.request().url().toString,
            method = response.request().method(),
            responseCode = response.code(),
            body = Some(body)
          )

          if (response.isSuccessful) {
            decode[BrazeUserResponse](body)
              .left.map(decodeError =>
                BrazeResponseFailure(s"Failed to decode successful response:$decodeError. Body to decode $body")
              )
          } else {
            Left(BrazeResponseFailure(s"The request to Braze was unsuccessful: ${response.code} - $body"))
          }
        })
  }

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

    Log.request(logger)(
      service = Log.Service.Braze,
      url = request.url().toString,
      method = request.method(),
      body = Some(body)
    )

    Try(
      HttpClient().newCall(request).execute()
    ).toEither
  }

  def handleRequestResult(logger: LambdaLogger)(result: Either[Throwable, Response]): Either[Failure, Unit] = {
    result
      .left.map(i => BrazeRequestFailure(s"Attempt to contact Braze failed with error: ${i.toString}"))
      .flatMap(response => {
        val body = response.body().string()

        Log.response(logger)(
          service = Log.Service.Braze,
          url = response.request().url().toString,
          method = response.request().method(),
          responseCode = response.code(),
          body = Some(body)
        )

        if (response.isSuccessful) {
          Right(())
        } else {
          Left(BrazeResponseFailure(s"The request to Braze was unsuccessful: ${response.code} - $body"))
        }
      })
  }

}
