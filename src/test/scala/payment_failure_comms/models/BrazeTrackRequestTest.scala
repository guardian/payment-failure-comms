package payment_failure_comms.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.time._

class BrazeTrackRequestTest extends AnyFlatSpec with should.Matchers {

  private def mkPaymentFailureRecord(id: Int, status: String) = {
    PaymentFailureRecordWithBrazeId(
      record = PaymentFailureRecord(
        Id = id.toString,
        Contact__c = "c7",
        Contact__r = SFContact(IdentityID__c = Some("i1")),
        SF_Subscription__r = SFSubscription(
          Product_Name__c = Some("prod1"),
          Zuora_Subscription_Name__c = Some("A-S87234234"),
          Cancellation_Request_Date__c = Some(OffsetDateTime.of(2021, 10, 25, 11, 15, 1, 0, ZoneOffset.ofHours(1)))
        ),
        Billing_Account__r = Billing(
          Zuora__BillToCountry__c = Some("United Kingdom")
        ),
        Payment_Failure_Type__c = Some("Credit Card"),
        STG_Initial_Gateway_Response_Code__c = Some("generic_decline"),
        STG_Initial_Gateway_Response__c = Some("Your card was declined."),
        PF_Comms_Status__c = status,
        PF_Comms_Last_Stage_Processed__c = None,
        PF_Comms_Number_of_Attempts__c = Some(0),
        Currency__c = "GBP",
        Invoice_Total_Amount__c = 1.2,
        Initial_Payment_Created_Date__c = None,
        Invoice_Created_Date__c = Some(LocalDate.of(2021, 10, 26)),
        Recovery_Date__c = Some(OffsetDateTime.of(2021, 10, 26, 11, 15, 1, 0, ZoneOffset.ofHours(1))),
        Cut_Off_Date__c = LocalDate.of(2021, 10, 27)
      ),
      brazeId = s"b$id"
    )
  }

  "Apply" should "succeed if no records" in {
    BrazeTrackRequest(records = Nil, zuoraAppId = "z1") shouldBe Right(Seq())
  }

  private val eventProperties = EventProperties(product = "prod1", currency = "GBP", amount = 1.2)

  it should "succeed if all records are valid" in {
    BrazeTrackRequest(
      records = Seq(
        mkPaymentFailureRecord(1, "Ready to send recovery event"),
        mkPaymentFailureRecord(2, "Ready to send voluntary cancel event"),
        mkPaymentFailureRecord(3, "Ready to send auto cancel event")
      ),
      zuoraAppId = "z1"
    ) shouldBe Right(
      Seq(
        BrazeTrackRequest(
          Seq(
            PaymentFailureTypeAttr("b1", Some("Credit Card")),
            ResponseCodeAttr("b1", Some("generic_decline")),
            ResponseMessageAttr("b1", Some("Your card was declined.")),
            RecoveryDateAttr("b1", Some(OffsetDateTime.of(2021, 10, 26, 11, 15, 1, 0, ZoneOffset.ofHours(1)))),
            SubscriptionIdAttr("b1", Some("A-S87234234")),
            ProductNameAttr("b1", "prod1"),
            InvoiceCreatedDateAttr("b1", Some(LocalDate.of(2021, 10, 26))),
            BillToCountryAttr("b1", Some("United Kingdom")),
            PaymentFailureTypeAttr("b2", Some("Credit Card")),
            ResponseCodeAttr("b2", Some("generic_decline")),
            ResponseMessageAttr("b2", Some("Your card was declined.")),
            RecoveryDateAttr("b2", Some(OffsetDateTime.of(2021, 10, 26, 11, 15, 1, 0, ZoneOffset.ofHours(1)))),
            SubscriptionIdAttr("b2", Some("A-S87234234")),
            ProductNameAttr("b2", "prod1"),
            InvoiceCreatedDateAttr("b2", Some(LocalDate.of(2021, 10, 26))),
            BillToCountryAttr("b2", Some("United Kingdom")),
            PaymentFailureTypeAttr("b3", Some("Credit Card")),
            ResponseCodeAttr("b3", Some("generic_decline")),
            ResponseMessageAttr("b3", Some("Your card was declined.")),
            RecoveryDateAttr("b3", Some(OffsetDateTime.of(2021, 10, 26, 11, 15, 1, 0, ZoneOffset.ofHours(1)))),
            SubscriptionIdAttr("b3", Some("A-S87234234")),
            ProductNameAttr("b3", "prod1"),
            InvoiceCreatedDateAttr("b3", Some(LocalDate.of(2021, 10, 26))),
            BillToCountryAttr("b3", Some("United Kingdom"))
          ),
          Seq(
            CustomEvent(
              external_id = "b1",
              app_id = "z1",
              name = "pf_recovery",
              time = "2021-10-26T00:00:00Z",
              properties = eventProperties
            ),
            CustomEvent(
              external_id = "b2",
              app_id = "z1",
              name = "pf_cancel_voluntary",
              time = "2021-10-25T10:15:01Z",
              properties = eventProperties
            ),
            CustomEvent(
              external_id = "b3",
              app_id = "z1",
              name = "pf_cancel_auto",
              time = "2021-10-27T00:00:00Z",
              properties = eventProperties
            )
          )
        )
      )
    )
  }

  it should "return multiple BrazeTrackRequest instances if more than 9 records" in {
    BrazeTrackRequest(
      records = Seq(
        mkPaymentFailureRecord(1, "Ready to send recovery event"),
        mkPaymentFailureRecord(2, "Ready to send voluntary cancel event"),
        mkPaymentFailureRecord(3, "Ready to send auto cancel event"),
        mkPaymentFailureRecord(4, "Ready to send recovery event"),
        mkPaymentFailureRecord(5, "Ready to send voluntary cancel event"),
        mkPaymentFailureRecord(6, "Ready to send auto cancel event"),
        mkPaymentFailureRecord(7, "Ready to send recovery event"),
        mkPaymentFailureRecord(8, "Ready to send voluntary cancel event"),
        mkPaymentFailureRecord(9, "Ready to send auto cancel event"),
        mkPaymentFailureRecord(10, "Ready to send recovery event"),
        mkPaymentFailureRecord(11, "Ready to send voluntary cancel event"),
        mkPaymentFailureRecord(12, "Ready to send auto cancel event")
      ),
      zuoraAppId = "z1"
    ) shouldBe Right(
      Seq(
        BrazeTrackRequest(
          Seq(
            PaymentFailureTypeAttr("b1", Some("Credit Card")),
            ResponseCodeAttr("b1", Some("generic_decline")),
            ResponseMessageAttr("b1", Some("Your card was declined.")),
            RecoveryDateAttr("b1", Some(OffsetDateTime.of(2021, 10, 26, 11, 15, 1, 0, ZoneOffset.ofHours(1)))),
            SubscriptionIdAttr("b1", Some("A-S87234234")),
            ProductNameAttr("b1", "prod1"),
            InvoiceCreatedDateAttr("b1", Some(LocalDate.of(2021, 10, 26))),
            BillToCountryAttr("b1", Some("United Kingdom")),
            PaymentFailureTypeAttr("b2", Some("Credit Card")),
            ResponseCodeAttr("b2", Some("generic_decline")),
            ResponseMessageAttr("b2", Some("Your card was declined.")),
            RecoveryDateAttr("b2", Some(OffsetDateTime.of(2021, 10, 26, 11, 15, 1, 0, ZoneOffset.ofHours(1)))),
            SubscriptionIdAttr("b2", Some("A-S87234234")),
            ProductNameAttr("b2", "prod1"),
            InvoiceCreatedDateAttr("b2", Some(LocalDate.of(2021, 10, 26))),
            BillToCountryAttr("b2", Some("United Kingdom")),
            PaymentFailureTypeAttr("b3", Some("Credit Card")),
            ResponseCodeAttr("b3", Some("generic_decline")),
            ResponseMessageAttr("b3", Some("Your card was declined.")),
            RecoveryDateAttr("b3", Some(OffsetDateTime.of(2021, 10, 26, 11, 15, 1, 0, ZoneOffset.ofHours(1)))),
            SubscriptionIdAttr("b3", Some("A-S87234234")),
            ProductNameAttr("b3", "prod1"),
            InvoiceCreatedDateAttr("b3", Some(LocalDate.of(2021, 10, 26))),
            BillToCountryAttr("b3", Some("United Kingdom")),
            PaymentFailureTypeAttr("b4", Some("Credit Card")),
            ResponseCodeAttr("b4", Some("generic_decline")),
            ResponseMessageAttr("b4", Some("Your card was declined.")),
            RecoveryDateAttr("b4", Some(OffsetDateTime.of(2021, 10, 26, 11, 15, 1, 0, ZoneOffset.ofHours(1)))),
            SubscriptionIdAttr("b4", Some("A-S87234234")),
            ProductNameAttr("b4", "prod1"),
            InvoiceCreatedDateAttr("b4", Some(LocalDate.of(2021, 10, 26))),
            BillToCountryAttr("b4", Some("United Kingdom")),
            PaymentFailureTypeAttr("b5", Some("Credit Card")),
            ResponseCodeAttr("b5", Some("generic_decline")),
            ResponseMessageAttr("b5", Some("Your card was declined.")),
            RecoveryDateAttr("b5", Some(OffsetDateTime.of(2021, 10, 26, 11, 15, 1, 0, ZoneOffset.ofHours(1)))),
            SubscriptionIdAttr("b5", Some("A-S87234234")),
            ProductNameAttr("b5", "prod1"),
            InvoiceCreatedDateAttr("b5", Some(LocalDate.of(2021, 10, 26))),
            BillToCountryAttr("b5", Some("United Kingdom")),
            PaymentFailureTypeAttr("b6", Some("Credit Card")),
            ResponseCodeAttr("b6", Some("generic_decline")),
            ResponseMessageAttr("b6", Some("Your card was declined.")),
            RecoveryDateAttr("b6", Some(OffsetDateTime.of(2021, 10, 26, 11, 15, 1, 0, ZoneOffset.ofHours(1)))),
            SubscriptionIdAttr("b6", Some("A-S87234234")),
            ProductNameAttr("b6", "prod1"),
            InvoiceCreatedDateAttr("b6", Some(LocalDate.of(2021, 10, 26))),
            BillToCountryAttr("b6", Some("United Kingdom")),
            PaymentFailureTypeAttr("b7", Some("Credit Card")),
            ResponseCodeAttr("b7", Some("generic_decline")),
            ResponseMessageAttr("b7", Some("Your card was declined.")),
            RecoveryDateAttr("b7", Some(OffsetDateTime.of(2021, 10, 26, 11, 15, 1, 0, ZoneOffset.ofHours(1)))),
            SubscriptionIdAttr("b7", Some("A-S87234234")),
            ProductNameAttr("b7", "prod1"),
            InvoiceCreatedDateAttr("b7", Some(LocalDate.of(2021, 10, 26))),
            BillToCountryAttr("b7", Some("United Kingdom")),
            PaymentFailureTypeAttr("b8", Some("Credit Card")),
            ResponseCodeAttr("b8", Some("generic_decline")),
            ResponseMessageAttr("b8", Some("Your card was declined.")),
            RecoveryDateAttr("b8", Some(OffsetDateTime.of(2021, 10, 26, 11, 15, 1, 0, ZoneOffset.ofHours(1)))),
            SubscriptionIdAttr("b8", Some("A-S87234234")),
            ProductNameAttr("b8", "prod1"),
            InvoiceCreatedDateAttr("b8", Some(LocalDate.of(2021, 10, 26))),
            BillToCountryAttr("b8", Some("United Kingdom")),
            PaymentFailureTypeAttr("b9", Some("Credit Card")),
            ResponseCodeAttr("b9", Some("generic_decline")),
            ResponseMessageAttr("b9", Some("Your card was declined.")),
            RecoveryDateAttr("b9", Some(OffsetDateTime.of(2021, 10, 26, 11, 15, 1, 0, ZoneOffset.ofHours(1)))),
            SubscriptionIdAttr("b9", Some("A-S87234234")),
            ProductNameAttr("b9", "prod1"),
            InvoiceCreatedDateAttr("b9", Some(LocalDate.of(2021, 10, 26))),
            BillToCountryAttr("b9", Some("United Kingdom"))
          ),
          Seq(
            CustomEvent(
              external_id = "b1",
              app_id = "z1",
              name = "pf_recovery",
              time = "2021-10-26T00:00:00Z",
              properties = eventProperties
            ),
            CustomEvent(
              external_id = "b2",
              app_id = "z1",
              name = "pf_cancel_voluntary",
              time = "2021-10-25T10:15:01Z",
              properties = eventProperties
            ),
            CustomEvent(
              external_id = "b3",
              app_id = "z1",
              name = "pf_cancel_auto",
              time = "2021-10-27T00:00:00Z",
              properties = eventProperties
            ),
            CustomEvent(
              external_id = "b4",
              app_id = "z1",
              name = "pf_recovery",
              time = "2021-10-26T00:00:00Z",
              properties = eventProperties
            ),
            CustomEvent(
              external_id = "b5",
              app_id = "z1",
              name = "pf_cancel_voluntary",
              time = "2021-10-25T10:15:01Z",
              properties = eventProperties
            ),
            CustomEvent(
              external_id = "b6",
              app_id = "z1",
              name = "pf_cancel_auto",
              time = "2021-10-27T00:00:00Z",
              properties = eventProperties
            ),
            CustomEvent(
              external_id = "b7",
              app_id = "z1",
              name = "pf_recovery",
              time = "2021-10-26T00:00:00Z",
              properties = eventProperties
            ),
            CustomEvent(
              external_id = "b8",
              app_id = "z1",
              name = "pf_cancel_voluntary",
              time = "2021-10-25T10:15:01Z",
              properties = eventProperties
            ),
            CustomEvent(
              external_id = "b9",
              app_id = "z1",
              name = "pf_cancel_auto",
              time = "2021-10-27T00:00:00Z",
              properties = eventProperties
            )
          )
        ),
        BrazeTrackRequest(
          Seq(
            PaymentFailureTypeAttr("b10", Some("Credit Card")),
            ResponseCodeAttr("b10", Some("generic_decline")),
            ResponseMessageAttr("b10", Some("Your card was declined.")),
            RecoveryDateAttr("b10", Some(OffsetDateTime.of(2021, 10, 26, 11, 15, 1, 0, ZoneOffset.ofHours(1)))),
            SubscriptionIdAttr("b10", Some("A-S87234234")),
            ProductNameAttr("b10", "prod1"),
            InvoiceCreatedDateAttr("b10", Some(LocalDate.of(2021, 10, 26))),
            BillToCountryAttr("b10", Some("United Kingdom")),
            PaymentFailureTypeAttr("b11", Some("Credit Card")),
            ResponseCodeAttr("b11", Some("generic_decline")),
            ResponseMessageAttr("b11", Some("Your card was declined.")),
            RecoveryDateAttr("b11", Some(OffsetDateTime.of(2021, 10, 26, 11, 15, 1, 0, ZoneOffset.ofHours(1)))),
            SubscriptionIdAttr("b11", Some("A-S87234234")),
            ProductNameAttr("b11", "prod1"),
            InvoiceCreatedDateAttr("b11", Some(LocalDate.of(2021, 10, 26))),
            BillToCountryAttr("b11", Some("United Kingdom")),
            PaymentFailureTypeAttr("b12", Some("Credit Card")),
            ResponseCodeAttr("b12", Some("generic_decline")),
            ResponseMessageAttr("b12", Some("Your card was declined.")),
            RecoveryDateAttr("b12", Some(OffsetDateTime.of(2021, 10, 26, 11, 15, 1, 0, ZoneOffset.ofHours(1)))),
            SubscriptionIdAttr("b12", Some("A-S87234234")),
            ProductNameAttr("b12", "prod1"),
            InvoiceCreatedDateAttr("b12", Some(LocalDate.of(2021, 10, 26))),
            BillToCountryAttr("b12", Some("United Kingdom"))
          ),
          Seq(
            CustomEvent(
              external_id = "b10",
              app_id = "z1",
              name = "pf_recovery",
              time = "2021-10-26T00:00:00Z",
              properties = eventProperties
            ),
            CustomEvent(
              external_id = "b11",
              app_id = "z1",
              name = "pf_cancel_voluntary",
              time = "2021-10-25T10:15:01Z",
              properties = eventProperties
            ),
            CustomEvent(
              external_id = "b12",
              app_id = "z1",
              name = "pf_cancel_auto",
              time = "2021-10-27T00:00:00Z",
              properties = eventProperties
            )
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
      zuoraAppId = "z1"
    ) shouldBe Left(SalesforceResponseFailure("Unexpected PF_Comms_Status__c value 'Unknown status' in PF record '2'"))
  }
}
