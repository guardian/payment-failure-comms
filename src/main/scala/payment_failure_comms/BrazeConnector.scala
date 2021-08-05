package payment_failure_comms

import okhttp3.{MediaType, OkHttpClient, Request, RequestBody, Response}
import payment_failure_comms.models.{BrazeConfig, BrazeRequestFailure, BrazeResponseFailure, Failure}
import scala.util.Try

object BrazeConnector {

  val client = new OkHttpClient()
  val JSON: MediaType = MediaType.get("application/json; charset=utf-8")

  def sendCustomEvent(brazeConfig: BrazeConfig, payload: String): Either[Failure, Unit] = {
    handleRequestResult(
      sendRequest(
        url = s"https://${brazeConfig.instanceUrl}/users/track",
        bearerToken = brazeConfig.bearerToken,
        payload = payload
      )
    )
  }

  def sendRequest(url: String, bearerToken: String, payload: String): Either[Throwable, Response] = {
    val body: RequestBody = RequestBody.create(payload, JSON)
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
