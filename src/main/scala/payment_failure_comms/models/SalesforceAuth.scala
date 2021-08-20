package payment_failure_comms.models

// Snake-cased for JSON marshalling
case class SalesforceAuth(access_token: String, instance_url: String)
