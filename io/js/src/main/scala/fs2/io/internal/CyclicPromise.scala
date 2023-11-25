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

package fs2.io.internal

import cats.effect.Async

/** A concurrent datatype similar to Scala Promise or Cats Effect Deferred,
  * but unsafe and reusable. It is reusable because [[complete]] and [[next]]
  * must be called in alternation to "reset" the state of the promise, but it
  * does not matter which is invoked first, so long as the other is invoked next.
  */
private[io] final class CyclicPromise[A] {
  private[this] var data: AnyRef = null

  private[this] def value = data.asInstanceOf[Either[Throwable, A]]
  private[this] def callback = data.asInstanceOf[Either[Throwable, A] => Unit]

  def complete(result: Either[Throwable, A]): Unit =
    if (data eq null) {
      data = result
    } else {
      callback(result)
      data = null
    }

  def next[F[_]](implicit F: Async[F]): F[A] = F.async { cb =>
    F.delay {
      if (data eq null) {
        data = cb
        Some(F.delay { data = null })
      } else {
        cb(value)
        data = null
        None
      }
    }
  }

}
