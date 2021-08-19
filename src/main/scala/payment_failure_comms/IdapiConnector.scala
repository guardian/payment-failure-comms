package payment_failure_comms

import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.parser.decode
import okhttp3.{MediaType, OkHttpClient, Request, Response}
import payment_failure_comms.models.{
  Failure,
  IdapiConfig,
  IdapiGetUserResponse,
  IdapiRequestFailure,
  IdapiResponseFailure
}
import scala.util.Try

object IdapiConnector {

  private val http = new OkHttpClient()

  def getBrazeId(idapiConfig: IdapiConfig, IdentityId: String): Either[Failure, String] = {
    handleRequestResult[IdapiGetUserResponse](
      getRequest(
        url = s"https://${idapiConfig.instanceUrl}/users/track",
        bearerToken = idapiConfig.bearerToken
      )
    ).map(response => response.user.privateFields.brazeUuid)
  }

  def getRequest(url: String, bearerToken: String): Either[Throwable, Response] = {
    val request: Request = new Request.Builder()
      .header("Authorization", s"Bearer ${bearerToken}")
      .url(url)
      .build()

    Try(
      http.newCall(request).execute()
    ).toEither
  }

  def handleRequestResult[T: Decoder](result: Either[Throwable, Response]): Either[Failure, T] = {
    result
      .left.map(i => IdapiRequestFailure(s"Attempt to contact Braze failed with error: ${i.toString}"))
      .flatMap(response =>
        if (response.isSuccessful) {
          decode[T](response.body().string())
            .left.map(decodeError =>
              IdapiResponseFailure(
                s"Failed to decode successful response:$decodeError. Body to decode ${response.body().string()}"
              )
            )
        } else {
          Left(IdapiResponseFailure(s"The request to Braze was unsuccessful: ${response.code} - ${response.body}"))
        }
      )
  }
}
