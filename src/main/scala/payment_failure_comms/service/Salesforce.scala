package payment_failure_comms.service

import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import okhttp3._
import payment_failure_comms.models._
import payment_failure_comms.{HttpClient, Log}
import zio._

trait Salesforce {
  def fetchPaymentFailureRecords: IO[SalesforceRequestFailure, Seq[PaymentFailureRecord]]
  def updatePaymentFailureRecords(request: PaymentFailureRecordUpdateRequest): IO[SalesforceRequestFailure, Unit]
}

object Salesforce {
  val fetchPaymentFailureRecords: ZIO[Salesforce, SalesforceRequestFailure, Seq[PaymentFailureRecord]] =
    ZIO.serviceWithZIO(_.fetchPaymentFailureRecords)

  def updatePaymentFailureRecords(
      request: PaymentFailureRecordUpdateRequest
  ): ZIO[Salesforce, SalesforceRequestFailure, Unit] =
    ZIO.serviceWithZIO(_.updatePaymentFailureRecords(request))
}

object SalesforceLive {

  // todo to go in config
  private val apiVersion = "TODO"

  private val JSON: MediaType = MediaType.get("application/json; charset=utf-8")
  private val urlEncoded = MediaType.parse("application/x-www-form-urlencoded")

  private val httpClient = HttpClient()

  private val genAuth = {

    def logAuthRequest(authDetails: String, request: Request) =
      Logging.logRequest(
        service = Log.Service.Salesforce,
        description = Some("Auth"),
        url = request.url().toString,
        method = request.method(),
        body = Some(authDetails)
      )

    for {
      logging <- ZIO.service[Logging]
      config <- ZIO.service[Config]
      sfConfig = config.salesforce
      authDetails = Seq(
        "grant_type" -> "password",
        "client_id" -> sfConfig.clientId,
        "client_secret" -> sfConfig.clientSecret,
        "username" -> sfConfig.username,
        "password" -> s"${sfConfig.password}${sfConfig.token}"
      )
        .map(_.productIterator.mkString("="))
        .mkString("&")
      requestBody = RequestBody.create(authDetails, urlEncoded)
      request = new Request.Builder()
        .url(s"${sfConfig.instanceUrl}/services/oauth2/token")
        .post(requestBody)
        .build()
      _ <- logAuthRequest(authDetails, request)
      responseBody <- call(request)
      auth <- ZIO
        .fromEither(decode[SalesforceAuth](responseBody)).mapError(ex => SalesforceRequestFailure(ex.getMessage))
    } yield auth
  }

  val layer: ZLayer[Logging with Config, SalesforceRequestFailure, Salesforce] = ZLayer.fromZIO(
    for {
      logging <- ZIO.service[Logging]
      config <- ZIO.service[Config]
      auth <- genAuth
    } yield new Salesforce {

      private def call(request: Request) =
        ZIO
          .scoped(
            for {
              response <- ZIO.acquireRelease(ZIO.attempt(httpClient.newCall(request).execute()))(response =>
                ZIO.succeed(response.close())
              )
              responseBody <- ZIO.succeed(response.body.string)
              _ <- logResponse(logging, response, responseBody)
            } yield responseBody
          ).mapError(ex => SalesforceRequestFailure(ex.getMessage))

      private def logReadRequest(logging: Logging, request: Request) =
        logging.logRequest(
          service = Log.Service.Salesforce,
          description = Some("Read outstanding payment failure records"),
          url = request.url().toString,
          method = request.method(),
          query = Some(Query.fetchPaymentFailures)
        )

      private def logWriteRequest(logging: Logging, request: Request, body: String) =
        logging.logRequest(
          service = Log.Service.Salesforce,
          description = Some("Update payment failure records"),
          url = request.url().toString,
          method = request.method(),
          body = Some(body)
        )

      private def logResponse(logging: Logging, response: Response, body: String) =
        logging.logResponse(
          service = Log.Service.Salesforce,
          url = response.request().url().toString,
          method = response.request().method(),
          responseCode = response.code(),
          body = Some(body)
        )

      val fetchPaymentFailureRecords: IO[SalesforceRequestFailure, Seq[PaymentFailureRecord]] = {
        val url = s"${auth.instance_url}/services/data/${config.salesforce.apiVersion}/query/"
        val urlWithParam = HttpUrl
          .parse(url)
          .newBuilder()
          .addQueryParameter("q", Query.fetchPaymentFailures)
          .build()
        val request = new Request.Builder()
          .header("Authorization", s"Bearer ${auth.access_token}")
          .url(urlWithParam)
          .get()
          .build()
        for {
          _ <- logReadRequest(logging, request)
          responseBody <- call(request)
          wrapper <- ZIO
            .fromEither(decode[SFPaymentFailureRecordWrapper](responseBody)).mapError(ex =>
              SalesforceRequestFailure(ex.getMessage)
            )
        } yield wrapper.records
      }

      override def updatePaymentFailureRecords(
          request: PaymentFailureRecordUpdateRequest
      ): IO[SalesforceRequestFailure, Unit] = {
        val b = request.asJson.toString
        val requestBody = RequestBody.create(b, JSON)
        val request2 = new Request.Builder()
          .header("Authorization", s"Bearer ${auth.access_token}").url(
            s"${auth.instance_url}/services/data/$apiVersion/composite/sobjects"
          ).patch(requestBody).build()
        for {
          _ <- logWriteRequest(logging, request2, b)
          _ <- call(request2)
        } yield ()
      }.unless(request.records.isEmpty).unit
    }
  )

  object Query {

    // Query limited to 200 records to avoid Salesforce's governor limits on number of requests per response
    val fetchPaymentFailures: String =
      """
            |SELECT Id,
            |   Status__c,
            |   Contact__r.IdentityID__c,
            |   SF_Subscription__r.Product_Name__c,
            |   PF_Comms_Last_Stage_Processed__c,
            |   PF_Comms_Number_of_Attempts__c,
            |   Currency__c,
            |   Invoice_Total_Amount__c
            |FROM Payment_Failure__c
            |WHERE PF_Comms_Status__c In ('Ready to process exit','Ready to process entry')
            |LIMIT 200""".stripMargin
  }
}
