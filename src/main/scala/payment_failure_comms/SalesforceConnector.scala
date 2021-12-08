package payment_failure_comms

import com.amazonaws.services.lambda.runtime.LambdaLogger
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import okhttp3._
import payment_failure_comms.models._

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import scala.language.implicitConversions
import scala.util.Try

class SalesforceConnector(authDetails: SalesforceAuth, apiVersion: String, logger: LambdaLogger) {
  def getRecordsToProcess(): Either[Failure, Seq[PaymentFailureRecord]] =
    SalesforceConnector.getRecordsToProcess(authDetails, apiVersion, logger)

  def updateRecords(request: PaymentFailureRecordUpdateRequest): Either[Failure, SFCompositeResponse] =
    SalesforceConnector.updateRecords(authDetails, apiVersion, request, logger)
}

object SalesforceConnector {

  private val urlEncoded = MediaType.parse("application/x-www-form-urlencoded")
  private val JSON: MediaType = MediaType.get("application/json; charset=utf-8")
  private val http = new OkHttpClient()

  implicit val salesforceDateTimeDecoder: Decoder[OffsetDateTime] =
    Decoder.decodeOffsetDateTimeWithFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxx"))

  def apply(sfConfig: SalesforceConfig, logger: LambdaLogger): Either[Failure, SalesforceConnector] = {
    auth(sfConfig, logger)
      .map(new SalesforceConnector(_, sfConfig.apiVersion, logger))
  }

  def auth(sfConfig: SalesforceConfig, logger: LambdaLogger): Either[Failure, SalesforceAuth] = {
    handleRequestResult[SalesforceAuth](logger)(
      authRequest(sfConfig, logger)
    )
  }

  def getRecordsToProcess(
      authDetails: SalesforceAuth,
      apiVersion: String,
      logger: LambdaLogger
  ): Either[Failure, Seq[PaymentFailureRecord]] = {
    /*
     * Query limited to 50 records to avoid hitting the limit for concurrent updates
     * using the Braze user track endpoint.
     * See https://www.braze.com/docs/api/errors/#fatal-errors
     */
    val query =
      """
      |SELECT 
      |  Id,
      |  Contact__c,
      |  Contact__r.IdentityID__c,
      |  SF_Subscription__r.Product_Name__c,
      |  SF_Subscription__r.Cancellation_Request_Date__c,
      |  PF_Comms_Status__c, 
      |  PF_Comms_Last_Stage_Processed__c, 
      |  PF_Comms_Number_of_Attempts__c,
      |  Currency__c,
      |  Invoice_Total_Amount__c,
      |  Initial_Payment_Created_Date__c,
      |  Last_Attempt_Date__c,
      |  Cut_Off_Date__c
      |FROM Payment_Failure__c
      |WHERE PF_Comms_Status__c
      |IN (
      |  'Ready to send entry event',
      |  'Ready to send recovery event',
      |  'Ready to send voluntary cancel event',
      |  'Ready to send auto cancel event'
      |)
      |LIMIT 50""".stripMargin

    handleRequestResult[SFPaymentFailureRecordWrapper](logger)(
      responseToQueryRequest(
        url = s"${authDetails.instance_url}/services/data/$apiVersion/query/",
        bearerToken = authDetails.access_token,
        query = query,
        logger = logger
      )
    )
      .map(_.records)
  }

  def updateRecords(
      authDetails: SalesforceAuth,
      apiVersion: String,
      request: PaymentFailureRecordUpdateRequest,
      logger: LambdaLogger
  ): Either[Failure, SFCompositeResponse] = {
    if (request.records.isEmpty)
      Right(SFCompositeResponse(Seq()))
    else
      handleRequestResult[Seq[SFResponse]](logger)(
        responseToCompositeRequest(logger)(
          url = s"${authDetails.instance_url}/services/data/$apiVersion/composite/sobjects",
          bearerToken = authDetails.access_token,
          body = request.asJson.toString
        )
      )
        .map(SFCompositeResponse.apply)
  }

  def authRequest(sfConfig: SalesforceConfig, logger: LambdaLogger): Either[Throwable, Response] = {
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

    Log.request(logger)(
      service = Log.Service.Salesforce,
      description = Some("Auth"),
      url = request.url().toString,
      method = request.method(),
      body = Some(authDetails)
    )

    Try(
      http.newCall(request).execute()
    ).toEither
  }

  def responseToQueryRequest(
      url: String,
      bearerToken: String,
      query: String,
      logger: LambdaLogger
  ): Either[Throwable, Response] = {

    val urlWithParam = HttpUrl
      .parse(url)
      .newBuilder()
      .addQueryParameter("q", query)
      .build()

    val request: Request = new Request.Builder()
      .header("Authorization", s"Bearer $bearerToken")
      .url(urlWithParam)
      .get()
      .build()

    Log.request(logger)(
      service = Log.Service.Salesforce,
      description = Some("Read outstanding payment failure records"),
      url = request.url().toString,
      method = request.method(),
      query = Some(query)
    )

    Try(
      http.newCall(request).execute()
    ).toEither
  }

  def responseToCompositeRequest(
      logger: LambdaLogger
  )(url: String, bearerToken: String, body: String): Either[Throwable, Response] = {
    val request: Request = new Request.Builder()
      .header("Authorization", s"Bearer $bearerToken")
      .url(url)
      .patch(RequestBody.create(body, JSON))
      .build()

    Log.request(logger)(
      service = Log.Service.Salesforce,
      description = Some("Update payment failure records"),
      url = request.url().toString,
      method = request.method(),
      body = Some(body)
    )

    Try(
      http.newCall(request).execute()
    ).toEither
  }

  def handleRequestResult[T: Decoder](logger: LambdaLogger)(result: Either[Throwable, Response]): Either[Failure, T] = {
    result
      .left.map(i => SalesforceRequestFailure(s"Attempt to contact Salesforce failed with error: ${i.toString}"))
      .flatMap(response => {
        val body = response.body().string()

        Log.response(logger)(
          service = Log.Service.Salesforce,
          url = response.request().url().toString,
          method = response.request().method(),
          responseCode = response.code(),
          body = Some(body)
        )

        if (response.isSuccessful) {
          decode[T](body)
            .left.map(decodeError =>
              SalesforceResponseFailure(s"Failed to decode successful response:$decodeError. Body to decode $body")
            )
        } else {
          Left(SalesforceResponseFailure(s"The request to Salesforce was unsuccessful: ${response.code} - $body"))
        }
      })
  }

}
