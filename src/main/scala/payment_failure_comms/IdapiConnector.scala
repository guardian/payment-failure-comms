package payment_failure_comms

import com.amazonaws.services.lambda.runtime.LambdaLogger
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.parser.decode
import okhttp3.{MediaType, OkHttpClient, Request, Response}
import payment_failure_comms.models._

import scala.util.Try

object IdapiConnector {

  private val http = new OkHttpClient()

  def getBrazeId(idapiConfig: IdapiConfig, logger: LambdaLogger)(IdentityId: String): Either[Failure, String] = {
    handleRequestResult[IdapiGetUserResponse](logger)(
      responseToGetRequest(logger)(
        url = s"https://${idapiConfig.instanceUrl}/user/${IdentityId}",
        bearerToken = idapiConfig.bearerToken
      )
    ).map(_.user.privateFields.brazeUuid)
  }

  def responseToGetRequest(logger: LambdaLogger)(url: String, bearerToken: String): Either[Throwable, Response] = {
    val request: Request = new Request.Builder()
      .header("Authorization", s"Bearer ${bearerToken}")
      .url(url)
      .build()

    Log.request(logger)(
      service = Log.Service.Idapi,
      description = Some("Read Braze UUID for an Identity ID"),
      url = request.url().toString,
      method = request.method()
    )

    Try(
      http.newCall(request).execute()
    ).toEither
  }

  def handleRequestResult[T: Decoder](logger: LambdaLogger)(result: Either[Throwable, Response]): Either[Failure, T] = {
    result
      .left.map(i => IdapiRequestFailure(s"Attempt to contact Braze failed with error: ${i.toString}"))
      .flatMap(response => {
        val body = response.body().string()

        Log.response(logger)(
          service = Log.Service.Idapi,
          url = response.request().url().toString,
          method = response.request().method(),
          responseCode = response.code(),
          body = Some(body)
        )

        if (response.isSuccessful) {
          decode[T](body)
            .left.map(decodeError =>
              IdapiResponseFailure(s"Failed to decode successful response:$decodeError. Body to decode ${body}")
            )
        } else {
          Left(IdapiResponseFailure(s"The request to Braze was unsuccessful: ${response.code} - ${body}"))
        }
      })
  }
}
