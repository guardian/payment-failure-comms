package payment_failure_comms

import io.circe.generic.auto._
import okhttp3.{MediaType, Request, Response, ResponseBody}
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import payment_failure_comms.models.{IdapiRequestFailure, IdapiResponseFailure}
import payment_failure_comms.testData.IdapiConnectorTestData.{
  ResponseModel,
  failureResponse,
  requestFailure,
  successfulResponse,
  unexpectedResponse,
  validBodyAsClass
}

class IdentityConnectorTests extends AnyFlatSpec with should.Matchers with EitherValues {

  // handleRequestResponse success cases
  "handleRequestResponse" should "return the corrrectly formed case class if the request was successul and the reply is 2xx" in {
    IdapiConnector.handleRequestResponse[ResponseModel](successfulResponse) shouldBe Right(validBodyAsClass)
  }

  // handleRequestResponse failure cases
  "handleRequestResponse" should "return an IdapiRequestFailure if the request was unsuccessful" in {
    val result = IdapiConnector.handleRequestResponse[ResponseModel](requestFailure)

    result.isLeft shouldBe true
    result.left.value shouldBe a[IdapiRequestFailure]
  }

  "handleRequestResponse" should "return an IdapiResponseFailure if the request was successful but an error code was received " in {
    val result = IdapiConnector.handleRequestResponse[ResponseModel](failureResponse)

    result.isLeft shouldBe true
    result.left.value shouldBe a[IdapiResponseFailure]
  }

  "handleRequestResponse" should "return an IdapiResponseFailure if the request was successful and the reply is 2xx but the body failed decoding " in {
    val result = IdapiConnector.handleRequestResponse[ResponseModel](unexpectedResponse)

    result.isLeft shouldBe true
    result.left.value shouldBe a[IdapiResponseFailure]
  }
}
