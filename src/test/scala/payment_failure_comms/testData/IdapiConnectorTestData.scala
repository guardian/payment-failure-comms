package payment_failure_comms.testData

import okhttp3.{MediaType, Protocol, Request, Response, ResponseBody}

object IdapiConnectorTestData {

  private val JSON: MediaType = MediaType.get("application/json; charset=utf-8")
  private val request = new Request.Builder().url("http://url").build()

  private val magicNumber = 32

  private val validBody = s"""{"field": $magicNumber}"""
  private val invalidBody = "{}"

  private val validResponseBody = ResponseBody.create(validBody, JSON)
  private val invalidResponseBody = ResponseBody.create(invalidBody, JSON)

  case class ResponseModel(field: Int)
  val validBodyAsClass = ResponseModel(32)

  val successfulResponse: Either[Throwable, Response] = Right(
    new Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(200)
      .message("OK")
      .body(validResponseBody)
      .build()
  )

  val requestFailure: Either[Throwable, Response] = Left(new Throwable())

  val failureResponse: Either[Throwable, Response] = Right(
    new Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(500)
      .message("NOT OK")
      .build()
  )

  val unexpectedResponse: Either[Throwable, Response] = Right(
    new Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(200)
      .message("OK")
      .body(invalidResponseBody)
      .build()
  )

}
