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
package reactivestreams

import cats.effect.kernel._
import cats.effect.std._
import cats.effect.syntax.all._
import cats.syntax.all._

import org.reactivestreams._

import scala.util.control.NoStackTrace

/** Implementation of a `org.reactivestreams.Publisher`
  *
  * This is used to publish elements from a `fs2.Stream` to a downstream reactivestreams system.
  *
  * @see [[https://github.com/reactive-streams/reactive-streams-jvm#1-publisher-code]]
  */
final class StreamPublisher[F[_], A] private (
    val stream: Stream[F, A],
    startDispatcher: Dispatcher[F],
    activeSubscriptions: AtomicCell[F, (Boolean, Set[Fiber[F, Throwable, Unit]])]
) (implicit
  F: Async[F]
) extends Publisher[A] {
  /** Subscribes to the given subscriber if this Publisher isn't canceled yet. */
  private def runSubscriber(subscriber: Subscriber[_ >: A]): F[Unit] =
    activeSubscriptions.evalModify[F[Unit]] {
      case (false, subscriptions) =>
        StreamSubscription.subscribe(stream, subscriber).start.map { subscription =>
          val newState =
            (false, subscriptions + subscription)

          val program =
            subscription.joinWithUnit >> activeSubscriptions.update {
              case (false, subscriptions) =>
                // When the subscriber finishes, remove its subscription from the active ones.
                (false, subscriptions - subscription)

              case (true, _) =>
                // This Publisher is canceled, do nothing.
                true -> Set.empty
            }

          newState -> program
        }

      case (true, _) =>
        // This Publisher is canceled, do nothing.
        F.pure((true, Set.empty[Fiber[F, Throwable, Unit]]) -> F.unit)
    }.flatten

  def subscribe(subscriber: Subscriber[_ >: A]): Unit = {
    nonNull(subscriber)
    startDispatcher.unsafeRunAndForget(runSubscriber(subscriber))
  }

  /** Cancels this publisher. */
  def cancel: F[Unit] =
    // First ensure this Publisher won't accept new subscribers.
    activeSubscriptions.getAndSet(true -> Set.empty).flatMap {
      case (false, subscriptions) =>
        // Cancel all active subscribers.
        subscriptions.toList.traverse_ { fiber =>
          fiber.cancel
        }

      case (true, _) =>
        // This publisher is already canceled, do nothing.
        F.unit
    }

  private def nonNull[B](b: B): Unit = if (b == null) throw new NullPointerException()
}

object StreamPublisher {
  def apply[F[_], A](
      stream: Stream[F, A]
  ) (implicit
    F: Async[F]
  ): Resource[F, StreamPublisher[F, A]] =
    (
      Dispatcher.sequential[F],
      Resource.eval(AtomicCell[F].of(false -> Set.empty[Fiber[F, Throwable, Unit]]))
    ).flatMapN {
      case (startDispatcher, activeSubscriptions) =>
        Resource.make(
          F.pure(new StreamPublisher(
            stream,
            startDispatcher,
            activeSubscriptions
          ))
        )(_.cancel)
    }

  private object CanceledStreamPublisherException extends IllegalStateException(
    "This StreamPublisher is not longer accepting subscribers"
  ) with NoStackTrace
}
