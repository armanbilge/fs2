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
package io.internal

import cats.effect.Async

import scala.scalajs.js

/** A concurrent datatype similar to FS2 Channel, but unsafe.
  */
private[io] final class Duct[A] {
  private[this] var data: js.Array[A] = new js.Array
  private[this] var callback: Either[Throwable, Chunk[A]] => Unit = null

  def send(value: A): Unit =
    if (callback eq null) {
      data.push(value)
      ()
    } else {
      callback(Right(Chunk.singleton(value)))
      callback = null
    }

  private[this] def next[F[_]](implicit F: Async[F]): F[Chunk[A]] = F.async { cb =>
    F.delay {
      if (data.length == 0) {
        callback = cb
        Some(F.delay { callback = null })
      } else {
        cb(Right(Chunk.from(new JSArraySeq(data))))
        data = new js.Array
        None
      }
    }
  }

  def stream[F[_]: Async]: Stream[F, A] =
    Stream.evalUnChunk(next).repeat

}

private final class JSArraySeq[A](array: js.Array[A]) extends collection.IndexedSeq[A] {
  def apply(i: Int) = array(i)
  def length = array.length
}
