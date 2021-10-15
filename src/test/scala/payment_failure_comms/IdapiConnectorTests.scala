package payment_failure_comms

import io.circe.generic.auto._
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import payment_failure_comms.models.{IdapiRequestFailure, IdapiResponseFailure}
import payment_failure_comms.testData.ConnectorTestData.{
  ResponseModel,
  failureResponse,
  requestFailure,
  successfulResponse,
  unexpectedResponse,
  validBodyAsClass
}

class IdapiConnectorTests extends AnyFlatSpec with should.Matchers with EitherValues {

  // handleRequestResult success cases
  "handleRequestResult" should "return the corrrectly formed case class if the request was successul and the reply is 2xx" in {
    IdapiConnector.handleRequestResult[ResponseModel](NoOpLogger())(successfulResponse) shouldBe Right(validBodyAsClass)
  }

  // handleRequestResult failure cases
  "handleRequestResult" should "return an IdapiRequestFailure if the request was unsuccessful" in {
    val result = IdapiConnector.handleRequestResult[ResponseModel](NoOpLogger())(requestFailure)

    result.isLeft shouldBe true
    result.left.value shouldBe a[IdapiRequestFailure]
  }

  "handleRequestResult" should "return an IdapiResponseFailure if the request was successful but an error code was received" in {
    val result = IdapiConnector.handleRequestResult[ResponseModel](NoOpLogger())(failureResponse)

    result.isLeft shouldBe true
    result.left.value shouldBe a[IdapiResponseFailure]
  }

  "handleRequestResult" should "return an IdapiResponseFailure if the request was successful and the reply is 2xx but the body failed decoding" in {
    val result = IdapiConnector.handleRequestResult[ResponseModel](NoOpLogger())(unexpectedResponse)

    result.isLeft shouldBe true
    result.left.value shouldBe a[IdapiResponseFailure]
  }
}
