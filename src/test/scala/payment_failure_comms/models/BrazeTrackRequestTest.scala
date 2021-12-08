package payment_failure_comms.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.time._

class BrazeTrackRequestTest extends AnyFlatSpec with should.Matchers {

  private def mkPaymentFailureRecord(id: Int, status: String) = {
    PaymentFailureRecordWithBrazeId(
      record = PaymentFailureRecord(
        Id = id.toString,
        Contact__r = SFContact(IdentityID__c = "i1"),
        SF_Subscription__r = SFSubscription(
          Product_Name__c = "prod1",
          Cancellation_Request_Date__c = Some(OffsetDateTime.of(2021, 10, 25, 11, 15, 1, 0, ZoneOffset.ofHours(1)))
        ),
        PF_Comms_Status__c = status,
        PF_Comms_Last_Stage_Processed__c = None,
        PF_Comms_Number_of_Attempts__c = Some(0),
        Currency__c = "GBP",
        Invoice_Total_Amount__c = 1.2,
        Initial_Payment_Created_Date__c = None,
        Last_Attempt_Date__c = Some(LocalDate.of(2021, 10, 26)),
        Cut_Off_Date__c = LocalDate.of(2021, 10, 27)
      ),
      brazeId = s"b$id"
    )
  }

  "Apply" should "succeed if no records" in {
    BrazeTrackRequest(
      records = Nil,
      zuoraAppId = "z1",
      eventsAlreadyWritten = BrazeUserResponse(Nil)
    ) shouldBe Right(BrazeTrackRequest(Nil))
  }

  it should "succeed if all records are valid" in {
    BrazeTrackRequest(
      records = Seq(
        mkPaymentFailureRecord(1, "Ready to send recovery event"),
        mkPaymentFailureRecord(2, "Ready to send voluntary cancel event"),
        mkPaymentFailureRecord(3, "Ready to send auto cancel event")
      ),
      zuoraAppId = "z1",
      eventsAlreadyWritten = BrazeUserResponse(Nil)
    ) shouldBe Right(
      BrazeTrackRequest(
        Seq(
          CustomEvent(
            external_id = "b1",
            app_id = "z1",
            name = "pf_recovery",
            time = "2021-10-26T00:00:00Z",
            properties = EventProperties(product = "prod1", currency = "GBP", amount = 1.2)
          ),
          CustomEvent(
            external_id = "b2",
            app_id = "z1",
            name = "pf_cancel_voluntary",
            time = "2021-10-25T10:15:01Z",
            properties = EventProperties(product = "prod1", currency = "GBP", amount = 1.2)
          ),
          CustomEvent(
            external_id = "b3",
            app_id = "z1",
            name = "pf_cancel_auto",
            time = "2021-10-27T00:00:00Z",
            properties = EventProperties(product = "prod1", currency = "GBP", amount = 1.2)
          )
        )
      )
    )
  }

  it should "fail if any record is invalid" in {
    BrazeTrackRequest(
      records = Seq(
        mkPaymentFailureRecord(1, "Ready to send recovery event"),
        mkPaymentFailureRecord(2, "Unknown status"),
        mkPaymentFailureRecord(3, "Ready to send auto cancel event")
      ),
      zuoraAppId = "z1",
      eventsAlreadyWritten = BrazeUserResponse(Nil)
    ) shouldBe Left(SalesforceResponseFailure("Unexpected PF_Comms_Status__c value 'Unknown status' in PF record '2'"))
  }

  "diff" should "give an empty list if no events" in {
    val events = Nil
    val eventsAlreadyWritten = BrazeUserResponse(Nil)
    BrazeTrackRequest.diff(events, eventsAlreadyWritten) shouldBe Nil
  }

  it should "give complete list of events if none already written" in {
    val events = Seq(
      CustomEvent(
        external_id = "ei1",
        app_id = "z1",
        name = "payment_failure",
        time = "2021-10-26T11:15:27Z",
        properties = EventProperties(product = "p1", currency = "GBP", amount = 1.2)
      )
    )
    val eventsAlreadyWritten = BrazeUserResponse(users =
      Seq(
        User(
          external_id = "ei2",
          custom_events = Some(
            Seq(
              UserCustomEvent(
                name = "payment_recovery",
                first = ZonedDateTime.of(2019, 10, 10, 4, 6, 12, 0, ZoneId.of("UTC")),
                last = ZonedDateTime.of(2020, 2, 3, 4, 5, 6, 0, ZoneId.of("UTC")),
                count = 3
              )
            )
          )
        )
      )
    )
    BrazeTrackRequest.diff(events, eventsAlreadyWritten) shouldBe Seq(
      CustomEvent(
        external_id = "ei1",
        app_id = "z1",
        name = "payment_failure",
        time = "2021-10-26T11:15:27Z",
        properties = EventProperties(product = "p1", currency = "GBP", amount = 1.2)
      )
    )
  }

  it should "miss out events that match those already written" in {
    val events = Seq(
      CustomEvent(
        external_id = "ei1",
        app_id = "z1",
        name = "payment_failure",
        time = "2021-10-26T11:15:27Z",
        properties = EventProperties(product = "p1", currency = "GBP", amount = 1.2)
      )
    )
    val eventsAlreadyWritten = BrazeUserResponse(users =
      Seq(
        User(
          external_id = "ei1",
          custom_events = Some(
            Seq(
              UserCustomEvent(
                name = "payment_failure",
                first = ZonedDateTime.of(2019, 10, 10, 4, 6, 12, 0, ZoneId.of("UTC")),
                last = ZonedDateTime.of(2021, 10, 26, 11, 15, 27, 0, ZoneId.of("UTC")),
                count = 3
              )
            )
          )
        )
      )
    )
    BrazeTrackRequest.diff(events, eventsAlreadyWritten) shouldBe Nil
  }

  it should "not miss out events where name doesn't match" in {
    val events = Seq(
      CustomEvent(
        external_id = "ei1",
        app_id = "z1",
        name = "payment_failure",
        time = "2021-10-26T11:15:27Z",
        properties = EventProperties(product = "p1", currency = "GBP", amount = 1.2)
      )
    )
    val eventsAlreadyWritten = BrazeUserResponse(users =
      Seq(
        User(
          external_id = "ei1",
          custom_events = Some(
            Seq(
              UserCustomEvent(
                name = "payment_recovery",
                first = ZonedDateTime.of(2019, 10, 10, 4, 6, 12, 0, ZoneId.of("UTC")),
                last = ZonedDateTime.of(2021, 10, 26, 11, 15, 27, 0, ZoneId.of("UTC")),
                count = 3
              )
            )
          )
        )
      )
    )
    BrazeTrackRequest.diff(events, eventsAlreadyWritten) shouldBe Seq(
      CustomEvent(
        external_id = "ei1",
        app_id = "z1",
        name = "payment_failure",
        time = "2021-10-26T11:15:27Z",
        properties = EventProperties(product = "p1", currency = "GBP", amount = 1.2)
      )
    )
  }

  it should "not miss out events where last time of same event was before the time of the event" in {
    val events = Seq(
      CustomEvent(
        external_id = "ei1",
        app_id = "z1",
        name = "payment_failure",
        time = "2021-10-26T11:15:27Z",
        properties = EventProperties(product = "p1", currency = "GBP", amount = 1.2)
      )
    )
    val eventsAlreadyWritten = BrazeUserResponse(users =
      Seq(
        User(
          external_id = "ei1",
          custom_events = Some(
            Seq(
              UserCustomEvent(
                name = "payment_failure",
                first = ZonedDateTime.of(2019, 10, 10, 4, 6, 12, 0, ZoneId.of("UTC")),
                last = ZonedDateTime.of(2020, 10, 26, 11, 15, 27, 0, ZoneId.of("UTC")),
                count = 3
              )
            )
          )
        )
      )
    )
    BrazeTrackRequest.diff(events, eventsAlreadyWritten) shouldBe Seq(
      CustomEvent(
        external_id = "ei1",
        app_id = "z1",
        name = "payment_failure",
        time = "2021-10-26T11:15:27Z",
        properties = EventProperties(product = "p1", currency = "GBP", amount = 1.2)
      )
    )
  }

  it should "miss out events where last time of same event was after the time of the event" in {
    val events = Seq(
      CustomEvent(
        external_id = "ei1",
        app_id = "z1",
        name = "payment_failure",
        time = "2021-10-26T11:15:27Z",
        properties = EventProperties(product = "p1", currency = "GBP", amount = 1.2)
      )
    )
    val eventsAlreadyWritten = BrazeUserResponse(users =
      Seq(
        User(
          external_id = "ei1",
          custom_events = Some(
            Seq(
              UserCustomEvent(
                name = "payment_failure",
                first = ZonedDateTime.of(2019, 10, 10, 4, 6, 12, 0, ZoneId.of("UTC")),
                last = ZonedDateTime.of(2021, 10, 27, 11, 15, 27, 0, ZoneId.of("UTC")),
                count = 3
              )
            )
          )
        )
      )
    )
    BrazeTrackRequest.diff(events, eventsAlreadyWritten) shouldBe Nil
  }
}
