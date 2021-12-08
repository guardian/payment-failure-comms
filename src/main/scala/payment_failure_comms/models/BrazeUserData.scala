package payment_failure_comms.models

import java.time.ZonedDateTime

/*
 * Braze response model for /users/export/id endpoint, serialized as Json.
 */
case class BrazeUserResponse(users: Seq[User])
case class User(external_id: String, custom_events: Option[Seq[UserCustomEvent]])
case class UserCustomEvent(name: String, first: ZonedDateTime, last: ZonedDateTime, count: Int)

/*
 * Braze request model for /users/export/id endpoint, serialized as Json.
 */
case class BrazeUserRequest(external_ids: Seq[String], fields_to_export: Seq[String])

object BrazeUserRequest {
  def fromPaymentFailureRecords(records: Seq[PaymentFailureRecordWithBrazeId]): BrazeUserRequest =
    BrazeUserRequest(
      external_ids = records.map(_.brazeId),
      fields_to_export = Seq("external_id", "custom_events")
    )
}
