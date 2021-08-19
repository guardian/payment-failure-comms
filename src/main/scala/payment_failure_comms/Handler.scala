package payment_failure_comms

import payment_failure_comms.models.Config

object Handler {

  def handleRequest(): Unit = {
    (for {
      config <- Config()
    } yield ()) match {
      case Left(_)  => println("Success")
      case Right(_) => println("I totally just ran.")
    }
  }

}
