package payment_failure_comms.testData

import okhttp3.{MediaType, Protocol, Request, Response, ResponseBody}

object ConnectorTestData {

  private val JSON: MediaType = MediaType.get("application/json; charset=utf-8")
  private val request = new Request.Builder().url("http://url").build()

  private val magicNumber = 32

  private val validBody = s"""{"field": $magicNumber}"""
  private val invalidBody = "{}"

  private def validResponseBody = ResponseBody.create(validBody, JSON)
  private def emptyResponseBody = ResponseBody.create("", JSON)
  private def invalidResponseBody = ResponseBody.create(invalidBody, JSON)

  case class ResponseModel(field: Int)
  val validBodyAsClass: ResponseModel = ResponseModel(magicNumber)

  def successfulResponse: Either[Throwable, Response] = Right(
    new Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(200)
      .message("OK")
      .body(validResponseBody)
      .build()
  )

  def requestFailure: Either[Throwable, Response] = Left(new Throwable())

  def failureResponse: Either[Throwable, Response] = Right(
    new Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(500)
      .message("NOT OK")
      .body(emptyResponseBody)
      .build()
  )

  def unexpectedResponse: Either[Throwable, Response] = Right(
    new Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(200)
      .message("OK")
      .body(invalidResponseBody)
      .build()
  )

}
