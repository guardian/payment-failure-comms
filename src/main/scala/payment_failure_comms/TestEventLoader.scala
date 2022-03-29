package payment_failure_comms

import okhttp3.{MediaType, OkHttpClient, Request, RequestBody}
import payment_failure_comms.TestEventLoader.mkCustomEvent
import payment_failure_comms.models.Config.getFromEnv
import payment_failure_comms.models._

import java.time.LocalDateTime
import scala.util.Try
import scala.util.chaining.scalaUtilChainingOps

/** Loads custom events into Braze to test the canvas logic.
  *
  * It takes three program arguments:
  *
  *   1. External ID of the test Braze account
  *
  * 2. Email address of the test Braze account
  *
  * 3. Number of scenario to generate, corresponding with below.
  *
  * The scenarios to test are:
  *
  *   1. Customer recovers within 4 days
  *
  * 2. Customer recovers between 4 days and 27 days
  *
  * 3. Customer recovers between 27 days and 28 days
  *
  * 4. Customer cancels voluntarily during PF
  *
  * 5. Customer is auto-cancelled
  *
  * 6. Customer in PF with two products concurrently
  *
  * 7. Customer in PF over two different periods where the first recovered and second is in progress
  *
  * 8. Customer in PF over two different periods with two different products where both products recovered in different
  * past periods
  */
object TestEventLoader extends App {

  private val externalId = args(0)
  private val emailAddress = args(1)
  private val scenario = args(2).toInt

  private val http = new OkHttpClient()

  private val maybeBrazeConfig =
    for {
      brazeInstanceUrl <- getFromEnv("brazeInstanceUrl")
      brazeApiKey <- getFromEnv("brazeApiKey")
      brazeZuoraAppId <- getFromEnv("appIdForBraze")
    } yield BrazeConfig(brazeInstanceUrl, brazeApiKey, brazeZuoraAppId)

  private def createAccount(config: BrazeConfig) =
    new Request.Builder()
      .header("Authorization", s"Bearer ${config.bearerToken}")
      .url(s"https://${config.instanceUrl}/users/track")
      .post(
        RequestBody.create(
          s"""{"attributes":["external_id":"$externalId","email":"$emailAddress"]}""",
          MediaType.get("application/json; charset=utf-8")
        )
      )
      .build().pipe(request =>
        Try(http.newCall(request).execute()).toEither.left.map(e => BrazeRequestFailure(e.getMessage))
      )

  private def deleteAccount(config: BrazeConfig) =
    new Request.Builder()
      .header("Authorization", s"Bearer ${config.bearerToken}")
      .url(s"https://${config.instanceUrl}/users/delete")
      .post(
        RequestBody.create(s"""{"external_ids":["$externalId"]}""", MediaType.get("application/json; charset=utf-8"))
      )
      .build()
      .pipe(request => Try(http.newCall(request).execute()).toEither.left.map(e => BrazeRequestFailure(e.getMessage)))

  private def mkCustomEvent(config: BrazeConfig, productName: String, eventName: String, daysBeforeNow: Int) =
    CustomEvent(
      external_id = externalId,
      app_id = config.zuoraAppId,
      name = eventName,
      time = LocalDateTime.now.minusDays(daysBeforeNow).toString,
      properties = EventProperties(product = productName, currency = "GBP", amount = 1)
    )

  private def genScenario(config: BrazeConfig, events: Seq[CustomEventWithAttributes]) = {
    val attributes = events.flatMap(_.attributes)
    val customEvents = events.map(_.event)

    BrazeConnector.sendCustomEvents(config, NoOpLogger())(payload = BrazeTrackRequest(attributes, customEvents))
  }

  private def withConfig[A](block: BrazeConfig => Either[Failure, A]): Unit =
    maybeBrazeConfig.flatMap(block) match {
      case Left(failure) => println(s"Failed: $failure")
      case Right(a)      => println(s"Succeeded: $a")
    }

  private def genScenario1(): Unit =
    withConfig(config =>
      genScenario(
        config,
        events = Seq(
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Digital Pack", "pf_entry", daysBeforeNow = 5)
          ),
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Digital Pack", "pf_recovery", daysBeforeNow = 1)
          )
        )
      )
    )

  private def genScenario2(): Unit =
    withConfig(config =>
      genScenario(
        config,
        events = Seq(
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Digital Pack", "pf_entry", daysBeforeNow = 15)
          ),
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Digital Pack", "pf_recovery", daysBeforeNow = 1)
          )
        )
      )
    )

  private def genScenario3(): Unit =
    withConfig(config =>
      genScenario(
        config,
        events = Seq(
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Digital Pack", "pf_entry", daysBeforeNow = 28)
          ),
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Digital Pack", "pf_recovery", daysBeforeNow = 1)
          )
        )
      )
    )

  private def genScenario4(): Unit =
    withConfig(config =>
      genScenario(
        config,
        events = Seq(
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Digital Pack", "pf_entry", daysBeforeNow = 12)
          ),
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Digital Pack", "pf_cancel_voluntary", daysBeforeNow = 1)
          )
        )
      )
    )

  private def genScenario5(): Unit =
    withConfig(config =>
      genScenario(
        config,
        events = Seq(
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Digital Pack", "pf_entry", daysBeforeNow = 28)
          ),
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Digital Pack", "pf_cancel_auto", daysBeforeNow = 1)
          )
        )
      )
    )

  private def genScenario6(): Unit =
    withConfig(config =>
      genScenario(
        config,
        events = Seq(
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Digital Pack", "pf_entry", daysBeforeNow = 4)
          ),
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Guardian Weekly", "pf_entry", daysBeforeNow = 3)
          )
        )
      )
    )

  private def genScenario7(): Unit =
    withConfig(config =>
      genScenario(
        config,
        events = Seq(
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Digital Pack", "pf_entry", daysBeforeNow = 90)
          ),
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Digital Pack", "pf_recovery", daysBeforeNow = 80)
          ),
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Digital Pack", "pf_entry", daysBeforeNow = 1)
          )
        )
      )
    )

  private def genScenario8(): Unit =
    withConfig(config =>
      genScenario(
        config,
        events = Seq(
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Digital Pack", "pf_entry", daysBeforeNow = 90)
          ),
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Digital Pack", "pf_recovery", daysBeforeNow = 80)
          ),
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Guardian Weekly", "pf_entry", daysBeforeNow = 60)
          ),
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Guardian Weekly", "pf_recovery", daysBeforeNow = 55)
          ),
          CustomEventWithAttributes(
            Seq(ResponseCodeAttr("b1", Option("402"))),
            mkCustomEvent(config, "Digital Pack", "pf_entry", daysBeforeNow = 1)
          )
        )
      )
    )

  withConfig { config =>
    deleteAccount(config)
    createAccount(config)
  }

  scenario match {
    case 1 => genScenario1()
    case 2 => genScenario2()
    case 3 => genScenario3()
    case 4 => genScenario4()
    case 5 => genScenario5()
    case 6 => genScenario6()
    case 7 => genScenario7()
    case 8 => genScenario8()
  }
}
