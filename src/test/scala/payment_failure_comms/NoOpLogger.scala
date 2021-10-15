package payment_failure_comms

import com.amazonaws.services.lambda.runtime.LambdaLogger

object NoOpLogger {
  def apply() = new LambdaLogger {
    def log(message: String): Unit = ()
    def log(message: Array[Byte]): Unit = ()
  }
}
