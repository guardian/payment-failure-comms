package payment_failure_comms.services

/*
import com.amazonaws.services.lambda.runtime.LambdaLogger
import io.circe.generic.auto._
import io.circe.parser.decode
import okhttp3._
import payment_failure_comms.Log
import payment_failure_comms.models._
import zio.{Console, Has, IO, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

trait Salesforce {
  def fetchPaymentFailureRecords: IO[SalesforceRequestFailure, Seq[PaymentFailureRecord]]
  def updatePaymentFailureRecords(request: PaymentFailureRecordUpdateRequest): IO[SalesforceResponseFailure, Unit]
}

object Salesforce {
  val fetchPaymentFailureRecords: ZIO[Has[Salesforce], SalesforceRequestFailure, Seq[PaymentFailureRecord]] =
    ZIO.serviceWith(_.fetchPaymentFailureRecords)

  def updatePaymentFailureRecords(
      request: PaymentFailureRecordUpdateRequest
  ): ZIO[Has[Salesforce], SalesforceResponseFailure, Unit] = ZIO.serviceWith(_.updatePaymentFailureRecords(request))
}

object SalesforceLive {

  private val urlEncoded = MediaType.parse("application/x-www-form-urlencoded")
  private val http = new OkHttpClient()

  private val auth: ZIO[Has[Configuration], SalesforceRequestFailure, SalesforceAuth] = {
    for {
      configService <- ZIO.service[Configuration]
      config <- configService.get
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
      body = RequestBody.create(authDetails, urlEncoded)
      request = new Request.Builder()
        .url(s"${sfConfig.instanceUrl}/services/oauth2/token")
        .post(body)
        .build()
      x <- ZIO.attempt(http.newCall(request).execute()).mapError(e => SalesforceRequestFailure(e.getMessage))
      body = x.body.string
      d <- ZIO.fromEither(decode[SalesforceAuth](body)).mapError(e => SalesforceRequestFailure(e.getMessage))
    } yield d
  }

  private val effect: ZIO[Has[Logging] with Has[Configuration], Failure, Salesforce] =
    for {
      logging <- ZIO.service[Logging]
      configService <- ZIO.service[Configuration]
      config <- configService.get
      a <- auth
    } yield new Salesforce {

      val fetchPaymentFailureRecords: IO[SalesforceRequestFailure, Seq[PaymentFailureRecord]] = {
        val url = s"${a.instance_url}/services/data/${config.salesforce.apiVersion}/query/"
        val urlWithParam = HttpUrl
          .parse(url)
          .newBuilder()
          .addQueryParameter("q", Query.fetchPaymentFailures)
          .build()
        val request = new Request.Builder()
          .header("Authorization", s"Bearer ${a.access_token}")
          .url(urlWithParam)
          .get()
          .build()
        for {
          _ <- logging.logRequest(
            service = Log.Service.Salesforce,
            description = Some("Read outstanding payment failure records"),
            url = request.url().toString,
            method = request.method(),
            query = Some(Query.fetchPaymentFailures)
          )
          // TODO should be closed after use
          response <- ZIO.attempt(http.newCall(request).execute()).mapError(e => SalesforceRequestFailure(e.getMessage))
          body = response.body().string()
          t <- ZIO
            .fromEither(decode[SFPaymentFailureRecordWrapper](body)).mapError(e =>
              SalesforceRequestFailure(e.getMessage)
            )
        } yield t.records
      }

      def updatePaymentFailureRecords(request: PaymentFailureRecordUpdateRequest): IO[SalesforceResponseFailure, Unit] =
        // TODO
        IO.succeed(())
    }

  val layer: ZLayer[Has[Logging] with Has[Configuration], Failure, Has[Salesforce]] = effect.toLayer

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

object SalesforceClient extends ZIOAppDefault {

  private val program = for {
    records <- Salesforce.fetchPaymentFailureRecords
    _ <- Console.printLine(records)
  } yield ()

  def run: ZIO[zio.ZEnv with Has[ZIOAppArgs], Any, Any] = {
    for {
      console <- ZIO.service[Console]
      lambdaLogger = new LambdaLogger {
        def log(message: String): Unit = console.printLine(message)
        def log(message: Array[Byte]): Unit = console.printLine(message)
      }
      _ <- program.injectCustom(LoggingLive.layer(lambdaLogger), ConfigurationLive.layer, SalesforceLive.layer)
    } yield ()
  }
}
 */
