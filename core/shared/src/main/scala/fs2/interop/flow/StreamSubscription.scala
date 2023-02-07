/*
 * Copyright (c) 2013 Functional Streams for Scala
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package fs2
package interop
package flow

import cats.effect.kernel.{Async, Deferred, Resource, Outcome}
import cats.effect.std.{Dispatcher, Queue}
import cats.effect.syntax.all._
import cats.syntax.all._

import java.util.concurrent.Flow.{Subscription, Subscriber}

/** Implementation of a [[Subscription]].
  *
  * This is used by the [[StreamUnicastPublisher]] to send elements from a [[Stream]] to a downstream reactive-streams system.
  *
  * @see [[https://github.com/reactive-streams/reactive-streams-jvm#3-subscription-code]]
  */
private[flow] final class StreamSubscription[F[_], A] private (
    stream: Stream[F, A],
    sub: Subscriber[A],
    requestDispatcher: Dispatcher[F],
    requests: Queue[F, StreamSubscription.Request],
    canceled: Deferred[F, Unit]
)(implicit F: Async[F])
    extends Subscription {
  // Ensure we are on a terminal state; i.e. set `canceled`, before signaling the subscriber.
  private def onError(ex: Throwable): F[Unit] =
    cancelMe >> F.delay(sub.onError(ex))

  private def onComplete: F[Unit] =
    cancelMe >> F.delay(sub.onComplete())

  private[flow] def run: F[Unit] = {
    def subscriptionPipe: Pipe[F, A, A] =
      in => {
        def go(s: Stream[F, A]): Pull[F, A, Unit] =
          Pull.eval(requests.take).flatMap {
            case StreamSubscription.Request.Infinite =>
              s.pull.echo

            case StreamSubscription.Request.Finite(n) =>
              s.pull.take(n).flatMap {
                case None      => Pull.done
                case Some(rem) => go(rem)
              }
          }

        go(in).stream
      }

    val events =
      stream
        .through(subscriptionPipe)
        .foreach(x => F.delay(sub.onNext(x)))
        .compile
        .drain

    events
      .race(canceled.get)
      .guaranteeCase {
        case Outcome.Succeeded(result) =>
          result.flatMap {
            case Left(())  => onComplete // Events finished normally.
            case Right(()) => F.unit // Events was canceled.
          }
        case Outcome.Errored(ex) => onError(ex)
        case Outcome.Canceled()  => cancelMe
      }
      .void
  }

  // According to the spec, it's acceptable for a concurrent cancel to not
  // be processed immediately, but if you have synchronous `cancel();
  // request()`, then the request _must_ be a NOOP. Fortunately,
  // ordering is guaranteed by a sequential dispatcher.
  // See https://github.com/zainab-ali/fs2-reactive-streams/issues/29
  // and https://github.com/zainab-ali/fs2-reactive-streams/issues/46
  private def cancelMe: F[Unit] =
    canceled.complete(()).void

  override def cancel(): Unit =
    try
      requestDispatcher.unsafeRunAndForget(cancelMe)
    catch {
      case _: IllegalStateException =>
      // Dispatcher already shutdown, we are on terminal state, NOOP.
    }

  override def request(n: Long): Unit = {
    val prog =
      canceled.tryGet.flatMap {
        case None =>
          if (n == java.lang.Long.MAX_VALUE)
            requests.offer(StreamSubscription.Request.Infinite)
          else if (n > 0)
            requests.offer(StreamSubscription.Request.Finite(n))
          else
            onError(
              ex = new IllegalArgumentException(s"Invalid number of elements [${n}]")
            )

        case Some(()) =>
          F.unit
      }
    try
      requestDispatcher.unsafeRunAndForget(prog)
    catch {
      case _: IllegalStateException =>
      // Dispatcher already shutdown, we are on terminal state, NOOP.
    }
  }
}

private[flow] object StreamSubscription {

  /** Represents a downstream subscriber's request to publish elements. */
  private sealed trait Request
  private object Request {
    case object Infinite extends Request
    final case class Finite(n: Long) extends Request
  }

  // Mostly for testing purposes.
  def apply[F[_], A](stream: Stream[F, A], subscriber: Subscriber[A])(implicit
      F: Async[F]
  ): Resource[F, StreamSubscription[F, A]] =
    (
      Dispatcher.sequential[F](await = true),
      Resource.eval(Queue.unbounded[F, Request]),
      Resource.eval(Deferred[F, Unit])
    ).mapN { case (requestDispatcher, requests, canceled) =>
      new StreamSubscription(
        stream,
        subscriber,
        requestDispatcher,
        requests,
        canceled
      )
    }.evalTap { subscription =>
      F.delay(subscriber.onSubscribe(subscription))
    }

  def subscribe[F[_], A](stream: Stream[F, A], subscriber: Subscriber[A])(implicit
      F: Async[F]
  ): F[Unit] =
    apply(stream, subscriber).use { subscription =>
      subscription.run
    }
}
