package payment_failure_comms.models

case class SFCompositeResponse(responses: Seq[SFResponse]) {
  lazy val hasErrors: Boolean = responses.exists(!_.success)
  lazy val errorsAsString: Option[String] =
    if (hasErrors) Some(s"Composite Request failed with: ${responses.filter(!_.success).map(_.errorAsString)}")
    else None
}

case class SFResponse(id: Option[String], success: Boolean, errors: Seq[SFResponseError]) {
  def errorAsString: Option[String] = if (success) None else Some(s"Errors: ${errors.map(_.errorAsString)}.")
}

case class SFResponseError(statusCode: String, message: String, fields: Seq[String]) {
  def errorAsString: String = s"statusCode: $statusCode; message: $message; fields: ${fields.mkString(", ")};"
}
