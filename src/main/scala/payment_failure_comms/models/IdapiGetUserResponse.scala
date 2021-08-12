package payment_failure_comms.models

case class IdapiGetUserResponse(user: IdapiUser)

case class IdapiUser(privateFields: IdapiPrivateFields)

case class IdapiPrivateFields(brazeUuid: String)
