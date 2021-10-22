package payment_failure_comms.services

import com.amazonaws.services.lambda.runtime.LambdaLogger
import io.circe.generic.auto._
import io.circe.parser.decode
import okhttp3._
import payment_failure_comms.Log
import payment_failure_comms.models.{
  PaymentFailureRecord,
  SFPaymentFailureRecordWrapper,
  SalesforceAuth,
  SalesforceRequestFailure
}
import zio.{Console, Has, IO, URIO, URLayer, ZIO, ZIOAppArgs, ZIOAppDefault}

import scala.util.Try

trait Salesforce {
  def fetchPaymentFailureRecords: IO[SalesforceRequestFailure, Seq[PaymentFailureRecord]]
}

object Salesforce {
  val fetchPaymentFailureRecords: ZIO[Has[Salesforce], SalesforceRequestFailure, Seq[PaymentFailureRecord]] =
    ZIO.serviceWith(_.fetchPaymentFailureRecords)
}

object SalesforceLive {

  private val urlEncoded = MediaType.parse("application/x-www-form-urlencoded")
  private val http = new OkHttpClient()

  private val auth: ZIO[Configuration, SalesforceRequestFailure, SalesforceAuth] = {
    for {
      configService <- ZIO.service[Configuration]
      config <- configService.get
      sfConfig = config.salesforce
    } yield {
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
  }

  private val effect: URIO[Has[Logging] with Has[Configuration], Salesforce] =
    for {
      logging <- ZIO.service[Logging]
      configService <- ZIO.service[Configuration]
      config <- configService.get
      a <- auth
    } yield new Salesforce {
      val fetchPaymentFailureRecords: IO[SalesforceRequestFailure, Seq[PaymentFailureRecord]] = {
        val url = s"${config.salesforce.instanceUrl}/services/data/${config.salesforce.apiVersion}/query/"
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

case class SalesforceLive(logging: Logging, configuration: Configuration) extends Salesforce {

  private val http = new OkHttpClient()

      private val auth: IO[SalesforceRequestFailure, SalesforceAuth] = ???

  val fetchPaymentFailureRecords: IO[SalesforceRequestFailure, Seq[PaymentFailureRecord]] = {
    for {
      config <- configuration.get
      a <- auth
      url = s"${config.salesforce.instanceUrl}/services/data/${config.salesforce.apiVersion}/query/"
      urlWithParam = HttpUrl
        .parse(url)
        .newBuilder()
        .addQueryParameter("q", Query.fetchPaymentFailures)
        .build()
      request = new Request.Builder()
        .header("Authorization", s"Bearer ${a.access_token}")
        .url(urlWithParam)
        .get()
        .build()
      _ <- logging.logRequest(
        service = Log.Service.Salesforce,
        description = Some("Read outstanding payment failure records"),
        url = request.url().toString,
        method = request.method(),
        query = Some(Query.fetchPaymentFailures)
      )
      response <- ZIO.attempt(http.newCall(request).execute()).mapError(e => SalesforceRequestFailure(e.getMessage))
      body = response.body().string()
      t <- ZIO
        .fromEither(decode[SFPaymentFailureRecordWrapper](body)).mapError(e => SalesforceRequestFailure(e.getMessage))
    } yield t.records
  }

  object Query {

        // Query limited to 200 records to avoid Salesforce's governor limits on number of requests per response
        val fetchPaymentFailures =
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
