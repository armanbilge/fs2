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
package io
package file

import cats.effect.kernel.Resource
import cats.effect.kernel.Temporal

import scala.concurrent.duration.FiniteDuration

/** Platform-agnostic methods for reading files.
  */
sealed trait ReadFiles[F[_]] {

  /** Reads all data from the file at the specified path.
    */
  def readAll(path: Path, chunkSize: Int): Stream[F, Byte]

  /** Returns a `ReadCursor` for the specified path.
    */
  def readCursor(path: Path): Resource[F, ReadCursor[F]]

  /** Reads a range of data synchronously from the file at the specified path.
    * `start` is inclusive, `end` is exclusive, so when `start` is 0 and `end` is 2,
    * two bytes are read.
    */
  def readRange(path: Path, chunkSize: Int, start: Long, end: Long): Stream[F, Byte]

  /** Returns an infinite stream of data from the file at the specified path.
    * Starts reading from the specified offset and upon reaching the end of the file,
    * polls every `pollDuration` for additional updates to the file.
    *
    * Read operations are limited to emitting chunks of the specified chunk size
    * but smaller chunks may occur.
    *
    * If an error occurs while reading from the file, the overall stream fails.
    */
  def tail(
      path: Path,
      chunkSize: Int,
      offset: Long = 0L,
      pollDelay: FiniteDuration
  ): Stream[F, Byte]

}

object ReadFiles {

  private[file] trait UnsealedReadFiles[F[_]] extends ReadFiles[F]

  private[file] abstract class TemporalReadFiles[F[_]](implicit F: Temporal[F])
      extends ReadFiles[F] {

    override def readRange(path: Path, chunkSize: Int, start: Long, end: Long): Stream[F, Byte] =
      Stream.resource(readCursor(path)).flatMap { cursor =>
        cursor.seek(start).readUntil(chunkSize, end).void.stream
      }

    override def tail(
        path: Path,
        chunkSize: Int,
        offset: Long,
        pollDelay: FiniteDuration
    ): Stream[F, Byte] =
      Stream.resource(readCursor(path)).flatMap { cursor =>
        cursor.seek(offset).tail(chunkSize, pollDelay).void.stream
      }

  }

}
