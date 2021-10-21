package payment_failure_comms.services

import payment_failure_comms.models.{PaymentFailureRecord, SFPaymentFailureRecordWrapper, SalesforceRequestFailure}
import zio.ZIO

trait Salesforce {
  def fetchPaymentFailureRecords: ZIO[Nothing, SalesforceRequestFailure, Seq[PaymentFailureRecord]]
}

case class SalesforceLive() extends Salesforce {
  val fetchPaymentFailureRecords: aZIO[Nothing, SalesforceRequestFailure, Seq[PaymentFailureRecord]] = {

    // Query limited to 200 records to avoid Salesforce's governor limits on number of requests per response
    val query =
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
        |WHERE PF_Comms_Status__c In ('', 'Ready to process exit','Ready to process entry')
        |LIMIT 200""".stripMargin

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
}
