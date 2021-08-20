package payment_failure_comms

import io.circe.Decoder
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.parser.decode
import okhttp3.{HttpUrl, MediaType, OkHttpClient, Request, RequestBody, Response}
import payment_failure_comms.models.{
  Failure,
  PaymentFailureRecord,
  SFCompositeResponse,
  SFPaymentFailureRecordWrapper,
  SFResponse,
  SalesforceAuth,
  SalesforceConfig,
  SalesforceRequestFailure,
  SalesforceResponseFailure
}

import scala.util.Try

case class PaymentFailureRecordUpdate()

class SalesforceConnector(authDetails: SalesforceAuth, apiVersion: String) {
  def getRecordsToProcess(): Either[Failure, Seq[PaymentFailureRecord]] =
    SalesforceConnector.getRecordsToProcess(authDetails, apiVersion)

  def updateRecords(records: Seq[PaymentFailureRecordUpdate]): Either[Failure, SFCompositeResponse] =
    SalesforceConnector.updateRecords(authDetails, apiVersion, records)
}

object SalesforceConnector {

  private val urlEncoded = MediaType.parse("application/x-www-form-urlencoded")
  private val JSON: MediaType = MediaType.get("application/json; charset=utf-8")
  private val http = new OkHttpClient()

  def apply(sfConfig: SalesforceConfig): Either[Failure, SalesforceConnector] = {
    auth(sfConfig)
      .map(new SalesforceConnector(_, sfConfig.apiVersion))
  }

  def auth(sfConfig: SalesforceConfig): Either[Failure, SalesforceAuth] = {
    handleRequestResult[SalesforceAuth](
      authRequest(sfConfig)
    )
  }

  def getRecordsToProcess(
      authDetails: SalesforceAuth,
      apiVersion: String
  ): Either[Failure, Seq[PaymentFailureRecord]] = {
    // TODO: Replace with actual query
    val query = "TBD"

    handleRequestResult[SFPaymentFailureRecordWrapper](
      queryRequest(
        url = s"${authDetails.instance_url}/services/data/$apiVersion/query/",
        bearerToken = authDetails.access_token,
        query = query
      )
    )
      .map(_.records)
  }

  def updateRecords(
      authDetails: SalesforceAuth,
      apiVersion: String,
      records: Seq[PaymentFailureRecordUpdate]
  ): Either[Failure, SFCompositeResponse] = {
    val body = RequestBody.create(records.asJson.toString, JSON)

    handleRequestResult[Seq[SFResponse]](
      compositeRequest(
        url = s"${authDetails.instance_url}/services/data/$apiVersion/composite/sobjects",
        bearerToken = authDetails.access_token,
        body
      )
    )
      .map(responses => SFCompositeResponse(responses))

  }

  def authRequest(sfConfig: SalesforceConfig): Either[Throwable, Response] = {
    val authDetails = Seq(
      "grant_type" -> "password",
      "client_id" -> sfConfig.clientId,
      "client_secret" -> sfConfig.clientSecret,
      "username" -> sfConfig.username,
      "password" -> s"${sfConfig.password}${sfConfig.token}"
    )
      .map(_.productIterator.mkString("="))
      .mkString("&")

    val body = RequestBody.create(authDetails, urlEncoded)

    val request = new Request.Builder()
      .url(s"${sfConfig.instanceUrl}/services/oauth2/token")
      .post(body)
      .build()

    Try(
      http.newCall(request).execute()
    ).toEither
  }

  def queryRequest(url: String, bearerToken: String, query: String): Either[Throwable, Response] = {
    val urlWithParam = HttpUrl
      .parse(url)
      .newBuilder()
      .addQueryParameter("q", query)
      .build();

    val request: Request = new Request.Builder()
      .header("Authorization", s"Bearer ${bearerToken}")
      .url(urlWithParam)
      .get()
      .build()

    Try(
      http.newCall(request).execute()
    ).toEither
  }

  def compositeRequest(url: String, bearerToken: String, body: RequestBody): Either[Throwable, Response] = {
    val request: Request = new Request.Builder()
      .header("Authorization", s"Bearer ${bearerToken}")
      .url(url)
      .patch(body)
      .build()

    Try(
      http.newCall(request).execute()
    ).toEither
  }

  def handleRequestResult[T: Decoder](result: Either[Throwable, Response]): Either[Failure, T] = {
    result
      .left.map(i => SalesforceRequestFailure(s"Attempt to contact Salesforce failed with error: ${i.toString}"))
      .flatMap { response =>
        val body = response.body().string()

        if (response.isSuccessful) {

          decode[T](body)
            .left.map(decodeError =>
              SalesforceResponseFailure(
                s"Failed to decode successful response:$decodeError. Body to decode ${body}"
              )
            )
        } else {
          Left(
            SalesforceResponseFailure(
              s"The request to Salesforce was unsuccessful: ${response.code} - ${body}"
            )
          )
        }
      }
  }

}
