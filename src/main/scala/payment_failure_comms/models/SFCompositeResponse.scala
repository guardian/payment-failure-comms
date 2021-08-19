package payment_failure_comms.models

case class SFCompositeResponse(responses: Seq[SFResponse]) {
  lazy val hasErrors: Boolean = responses.exists(!_.success)
  lazy val errorsAsString: Option[String] =
    if (hasErrors) Some(s"Composite Request failed with: ${responses.map(a => a.errorAsString)}") else None
}

case class SFResponse(id: Option[String], success: Boolean, errors: Seq[SFResponseError]) {
  def errorAsString: Option[String] = if (success) None else Some(s"Errors: ${errors.map(a => a.errorAsString)}.")
}

case class SFResponseError(statusCode: String, message: String, fields: Seq[String]) {
  def errorAsString: String = s"statusCode: $statusCode; message: $message; fields: ${fields.mkString(", ")};"
}
