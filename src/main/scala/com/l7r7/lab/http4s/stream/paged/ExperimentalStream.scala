package com.l7r7.lab.http4s.stream.paged

import cats.implicits._
import fs2.{ Pull, Stream }

object ExperimentalStream {
  /** this will be part of fs2: https://github.com/functional-streams-for-scala/fs2/pull/1594/
   */
  def unfoldLoopEval[F[_], S, O](s: S)(f: S => F[(O, Option[S])]): Stream[F, O] =
    Pull
      .loop[F, O, S](
        s =>
          Pull.eval(f(s)).flatMap {
            case (o, sOpt) => Pull.output1(o) >> Pull.pure(sOpt)
          }
      )(s)
      .void
      .stream
}
