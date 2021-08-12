package payment_failure_comms.models

case class Config(idapi: IdapiConfig, braze: BrazeConfig)

case class IdapiConfig(instanceUrl: String, bearerToken: String)

case class BrazeConfig(instanceUrl: String, bearerToken: String, zuoraAppId: String)

object Config {
  def apply(): Either[Failure, Config] = {
    def getFromEnv(prop: String): Either[ConfigFailure, String] =
      sys.env.get(prop).toRight(ConfigFailure(s"Could not obtain $prop"))

    for {
      brazeInstanceUrl <- getFromEnv("brazeInstanceUrl")
      brazeBearerToken <- getFromEnv("brazeBearerToken")
      brazeZuoraAppId <- getFromEnv("zuoraAppIdForBraze")
      idapiInstanceUrl <- getFromEnv("idapiInstanceUrl")
      idapiBearerToken <- getFromEnv("idapiBearerToken")
    } yield Config(
      IdapiConfig(idapiInstanceUrl, idapiBearerToken),
      BrazeConfig(brazeInstanceUrl, brazeBearerToken, brazeZuoraAppId)
    )
  }
}
