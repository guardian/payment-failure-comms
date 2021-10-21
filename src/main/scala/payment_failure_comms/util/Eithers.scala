package payment_failure_comms.util

import payment_failure_comms.models.Failure

object Eithers {

  /*
   * Converts seq of Either to either the first left or the seq of rights.
   */
  def seqToEither[E, A](eithers: Seq[Either[E, A]]): Either[E, Seq[A]] =
    eithers
      .collectFirst { case Left(e) =>
        Left(e)
      }.getOrElse(
        Right(eithers.collect { case Right(a) => a })
      )
}
