package payment_failure_comms.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import payment_failure_comms.{BrazeConnector, NoOpLogger}
import payment_failure_comms.testData.ConnectorTestData.successfulResponse

import java.time.{LocalDate, OffsetDateTime, ZoneOffset}

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
        Status__c = status,
        PF_Comms_Status__c = "Ready to process exit",
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
      zuoraAppId = "z1"
    ) shouldBe Right(BrazeTrackRequest(Nil))
  }

  it should "succeed if all records are valid" in {
    BrazeTrackRequest(
      records = Seq(
        mkPaymentFailureRecord(1, "Recovered"),
        mkPaymentFailureRecord(2, "Failed"),
        mkPaymentFailureRecord(3, "Auto-Cancel Failure")
      ),
      zuoraAppId = "z1"
    ) shouldBe Right(
      BrazeTrackRequest(
        Seq(
          CustomEvent(
            external_id = "b1",
            app_id = "z1",
            name = "payment_recovery",
            time = "2021-10-26T00:00:00Z",
            properties = EventProperties(product = "prod1", currency = "GBP", amount = 1.2)
          ),
          CustomEvent(
            external_id = "b2",
            app_id = "z1",
            name = "",
            time = "2021-10-25T10:15:01Z",
            properties = EventProperties(product = "prod1", currency = "GBP", amount = 1.2)
          ),
          CustomEvent(
            external_id = "b3",
            app_id = "z1",
            name = "payment_failure_cancelation",
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
        mkPaymentFailureRecord(1, "Recovered"),
        mkPaymentFailureRecord(2, "Unknown status"),
        mkPaymentFailureRecord(3, "Auto-Cancel Failure")
      ),
      zuoraAppId = "z1"
    ) shouldBe Left(SalesforceResponseFailure("Unexpected status value 'Unknown status' in PF record 2"))
  }
}
