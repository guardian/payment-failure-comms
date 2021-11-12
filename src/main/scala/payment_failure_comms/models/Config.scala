package payment_failure_comms.models

case class Config(salesforce: SalesforceConfig, idapi: IdapiConfig, braze: BrazeConfig)

case class SalesforceConfig(
    instanceUrl: String,
    apiVersion: String,
    clientId: String,
    clientSecret: String,
    username: String,
    password: String,
    token: String
)

case class IdapiConfig(instanceUrl: String, bearerToken: String)

case class BrazeConfig(instanceUrl: String, bearerToken: String, zuoraAppId: String)

object Config {

  def getFromEnv(prop: String): Either[ConfigFailure, String] =
    sys.env.get(prop).toRight(ConfigFailure(s"Could not obtain $prop"))

  def apply(): Either[Failure, Config] = {
    for {
      salesforceInstanceUrl <- getFromEnv("salesforceInstanceUrl")
      salesforceApiVersion <- getFromEnv("salesforceApiVersion")
      salesforceClientId <- getFromEnv("salesforceClientId")
      salesforceClientSecret <- getFromEnv("salesforceClientSecret")
      salesforceUsername <- getFromEnv("salesforceUsername")
      salesforcePassword <- getFromEnv("salesforcePassword")
      salesforceToken <- getFromEnv("salesforceToken")
      brazeInstanceUrl <- getFromEnv("brazeInstanceUrl")
      brazeBearerToken <- getFromEnv("brazeBearerToken")
      brazeZuoraAppId <- getFromEnv("zuoraAppIdForBraze")
      idapiInstanceUrl <- getFromEnv("idapiInstanceUrl")
      idapiBearerToken <- getFromEnv("idapiBearerToken")
    } yield Config(
      SalesforceConfig(
        salesforceInstanceUrl,
        salesforceApiVersion,
        salesforceClientId,
        salesforceClientSecret,
        salesforceUsername,
        salesforcePassword,
        salesforceToken
      ),
      IdapiConfig(idapiInstanceUrl, idapiBearerToken),
      BrazeConfig(brazeInstanceUrl, brazeBearerToken, brazeZuoraAppId)
    )
  }
}
