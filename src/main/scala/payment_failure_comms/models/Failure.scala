package payment_failure_comms.models

sealed trait Failure {
  def kind: String
  def details: String
}

case class ConfigFailure(details: String) extends Failure {
  val kind: String = "Config"
}

case class IdapiRequestFailure(details: String) extends Failure {
  val kind: String = "IDAPI Request"
}

case class IdapiResponseFailure(details: String) extends Failure {
  val kind: String = "IDAPI Response"
}

case class BrazeRequestFailure(details: String) extends Failure {
  val kind: String = "Braze Request"
}

case class BrazeResponseFailure(details: String) extends Failure {
  val kind: String = "Braze Response"
}
