package payment_failure_comms

import okhttp3.{MediaType, OkHttpClient, Request, RequestBody, Response}
import payment_failure_comms.models.{BrazeConfig, BrazeTrackRequest, BrazeRequestFailure, BrazeResponseFailure, Failure}
import scala.util.Try
import io.circe.generic.auto._
import io.circe.syntax._

object BrazeConnector {

  val client = new OkHttpClient()
  val JSON: MediaType = MediaType.get("application/json; charset=utf-8")

  def sendCustomEvent(brazeConfig: BrazeConfig, payload: BrazeTrackRequest): Either[Failure, Unit] = {
    handleRequestResult(
      sendRequest(
        url = s"https://${brazeConfig.instanceUrl}/users/track",
        bearerToken = brazeConfig.bearerToken,
        RequestBody.create(payload.asJson.toString, JSON)
      )
    )
  }

  def sendRequest(url: String, bearerToken: String, body: RequestBody): Either[Throwable, Response] = {
    val request: Request = new Request.Builder()
      .header("Authorization", s"Bearer ${bearerToken}")
      .url(url)
      .post(body)
      .build()

    Try(
      client.newCall(request).execute()
    ).toEither
  }

  def handleRequestResult(result: Either[Throwable, Response]): Either[Failure, Unit] = {
    result
      .left.map(i => BrazeRequestFailure(s"Attempt to contact Braze failed with error: ${i.toString}"))
      .flatMap(response =>
        if (response.isSuccessful) {
          Right(())
        } else {
          Left(BrazeResponseFailure(s"The request to Braze was unsuccessful: ${response.code} - ${response.body}"))
        }
      )
  }

}
