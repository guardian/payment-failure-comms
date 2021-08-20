package payment_failure_comms

import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import payment_failure_comms.models.{BrazeRequestFailure, BrazeResponseFailure}
import payment_failure_comms.testData.ConnectorTestData.{failureResponse, requestFailure, successfulResponse}

class BrazeConnectorTests extends AnyFlatSpec with should.Matchers with EitherValues {

  // handleRequestResult success cases
  "handleRequestResult" should "return Unit if the request was successul and the reply is 2xx" in {
    BrazeConnector.handleRequestResult(successfulResponse) shouldBe Right(())
  }

  // handleRequestResult failure cases
  "handleRequestResult" should "return a BrazeRequestFailure if the request was unsuccessful" in {
    val result = BrazeConnector.handleRequestResult(requestFailure)

    result.isLeft shouldBe true
    result.left.value shouldBe a[BrazeRequestFailure]
  }

  "handleRequestResult" should "return a BrazeResponseFailure if the request was successful but an error code was received " in {
    val result = BrazeConnector.handleRequestResult(failureResponse)

    result.isLeft shouldBe true
    result.left.value shouldBe a[BrazeResponseFailure]
  }

}
