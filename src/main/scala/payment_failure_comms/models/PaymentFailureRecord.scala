package payment_failure_comms.models

import java.time.{LocalDate, OffsetDateTime}

case class PaymentFailureRecord(
    Id: String,
    Contact__c: String,
    Contact__r: SFContact,
    SF_Subscription__r: SFSubscription,
    Billing_Account__r: Billing,
    PF_Comms_Status__c: String,
    Payment_Failure_Type__c: String,
    Initial_Payment__r: SFPayment,
    PF_Comms_Last_Stage_Processed__c: Option[String] = None,
    PF_Comms_Number_of_Attempts__c: Option[Int] = Some(0),
    Currency__c: String,
    Invoice_Total_Amount__c: Double,
    Invoice_Created_Date__c: Option[LocalDate],
    Initial_Payment_Created_Date__c: Option[OffsetDateTime],
    Last_Attempt_Date__c: Option[LocalDate],
    Cut_Off_Date__c: LocalDate
)

case class SFPayment(Zuora__GatewayResponseCode__c: String, Zuora__GatewayResponse__c: String)

case class SFContact(IdentityID__c: Option[String], FirstName: String, LastName: String)

case class SFSubscription(
    Product_Name__c: String,
    Zuora_Subscription_Name__c: String,
    Cancellation_Request_Date__c: Option[OffsetDateTime] = None
)

case class Billing(
    Zuora__BillToCountry__c: String
)

case class SFPaymentFailureRecordWrapper(totalSize: Int, done: Boolean, records: Seq[PaymentFailureRecord])
