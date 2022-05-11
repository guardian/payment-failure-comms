package payment_failure_comms.models

case class Config(stage: String, salesforce: SalesforceConfig, idapi: IdapiConfig, braze: BrazeConfig)

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

  def apply(): Either[ConfigFailure, Config] = {
    for {
      stage <- getFromEnv("stage")
      salesforceInstanceUrl <- getFromEnv("salesforceInstanceUrl")
      salesforceApiVersion <- getFromEnv("salesforceApiVersion")
      salesforceClientId <- getFromEnv("salesforceClientId")
      salesforceClientSecret <- getFromEnv("salesforceClientSecret")
      salesforceUsername <- getFromEnv("salesforceUserName")
      salesforcePassword <- getFromEnv("salesforcePassword")
      salesforceToken <- getFromEnv("salesforceToken")
      brazeInstanceUrl <- getFromEnv("brazeInstanceUrl")
      brazeApiKey <- getFromEnv("brazeApiKey")
      appIdForBraze <- getFromEnv("appIdForBraze")
      idapiInstanceUrl <- getFromEnv("idapiInstanceUrl")
      idapiBearerToken <- getFromEnv("idapiBearerToken")
    } yield Config(
      stage,
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
      BrazeConfig(brazeInstanceUrl, brazeApiKey, appIdForBraze)
    )
  }
}
