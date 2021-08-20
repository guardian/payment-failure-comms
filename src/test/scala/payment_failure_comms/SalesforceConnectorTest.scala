package payment_failure_comms

import io.circe.generic.auto._
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import payment_failure_comms.models.{SalesforceRequestFailure, SalesforceResponseFailure}
import payment_failure_comms.testData.ConnectorTestData.{
  ResponseModel,
  failureResponse,
  requestFailure,
  successfulResponse,
  unexpectedResponse,
  validBodyAsClass
}

class SalesforceConnectorTest extends AnyFlatSpec with should.Matchers with EitherValues {

  // handleRequestResult success cases
  "handleRequestResult" should "return the correctly formed case class if the request was successul and the reply is 2xx" in {
    SalesforceConnector.handleRequestResult[ResponseModel](successfulResponse) shouldBe Right(validBodyAsClass)
  }

  // handleRequestResult failure cases
  "handleRequestResult" should "return a SalesforceRequestFailure if the request was unsuccessful" in {
    val result = SalesforceConnector.handleRequestResult[ResponseModel](requestFailure)

    result.isLeft shouldBe true
    result.left.value shouldBe a[SalesforceRequestFailure]
  }

  "handleRequestResult" should "return a SalesforceResponseFailure if the request was successful but an error code was received " in {
    val result = SalesforceConnector.handleRequestResult[ResponseModel](failureResponse)

    result.isLeft shouldBe true
    result.left.value shouldBe a[SalesforceResponseFailure]
  }

  "handleRequestResult" should "return a SalesforceResponseFailure if the request was successful and the reply is 2xx but the body failed decoding " in {
    val result = SalesforceConnector.handleRequestResult[ResponseModel](unexpectedResponse)

    println(result)

    result.isLeft shouldBe true
    result.left.value shouldBe a[SalesforceResponseFailure]
  }

}
