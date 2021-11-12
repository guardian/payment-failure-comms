package payment_failure_comms

import com.amazonaws.services.lambda.runtime.LambdaLogger
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import payment_failure_comms.models.Failure

object Log {

  private sealed trait LogLevel { def name: String }
  private object LogLevel {
    case object Info extends LogLevel {
      val name = "INFO"
    }
    case object Error extends LogLevel {
      val name = "ERROR"
    }
  }

  sealed trait Service { def name: String }
  object Service {
    case object Salesforce extends Service {
      val name = "Salesforce"
    }
    case object Braze extends Service {
      val name = "Braze"
    }
    case object Idapi extends Service {
      val name = "Idapi"
    }
  }

  private case class InfoMessage(
      logLevel: String = LogLevel.Info.name,
      event: String,
      description: Option[String] = None,
      service: Option[String] = None,
      url: Option[String] = None,
      httpMethod: Option[String] = None,
      query: Option[String] = None,
      body: Option[String] = None,
      responseCode: Option[Int] = None
  )
  private case class ErrorMessage(logLevel: String = LogLevel.Error.name, failure: String)

  private def append(logger: LambdaLogger)(json: Json): Unit = logger.log(json.dropNullValues.noSpaces)
  private def info(logger: LambdaLogger)(message: InfoMessage): Unit = append(logger)(message.asJson)
  private def error(logger: LambdaLogger)(message: ErrorMessage): Unit = append(logger)(message.asJson)

  def failure(logger: LambdaLogger)(failure: Failure): Unit =
    error(logger)(ErrorMessage(failure = failure.details))

  def request(logger: LambdaLogger)(
      service: Service,
      description: Option[String] = None,
      url: String,
      method: String,
      query: Option[String] = None,
      body: Option[String] = None
  ): Unit =
    info(logger)(
      InfoMessage(
        event = "Request",
        service = Some(service.name),
        description = description,
        url = Some(url),
        httpMethod = Some(method),
        query = query,
        body = body
      )
    )

  def response(logger: LambdaLogger)(
      service: Service,
      description: Option[String] = None,
      url: String,
      method: String,
      query: Option[String] = None,
      responseCode: Int,
      body: Option[String] = None
  ): Unit =
    info(logger)(
      InfoMessage(
        event = "Response",
        service = Some(service.name),
        description = description,
        url = Some(url),
        httpMethod = Some(method),
        query = query,
        responseCode = Some(responseCode),
        body = body
      )
    )

  def completion(logger: LambdaLogger)(): Unit =
    info(logger)(InfoMessage(event = "Completion"))
}

object ConsoleLogger {
  def apply(): LambdaLogger = new LambdaLogger {
    def log(message: String): Unit = println(message)
    def log(message: Array[Byte]): Unit = println(message)
  }
}

object NoOpLogger {
  def apply(): LambdaLogger = new LambdaLogger {
    def log(message: String): Unit = ()
    def log(message: Array[Byte]): Unit = ()
  }
}
