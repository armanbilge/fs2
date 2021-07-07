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

import fs2.js.node.bufferMod.global.Buffer
import fs2.js.node.streamMod.Readable
import fs2.js.node.streamMod.Writable
import cats.effect.std.Dispatcher
import cats.effect.kernel.Async
import cats.effect.syntax.all._
import fs2.io.internal.ByteChunkOps._
import fs2.io.internal.EventEmitterOps._
import fs2.js.node.nodeStrings
import cats.effect.kernel.Deferred
import cats.effect.std.Queue
import scala.scalajs.js
import cats.effect.kernel.Resource

private[fs2] trait ioplatform {

  def fromWritable[F[_]](writable: Writable)(implicit F: Async[F]): Pipe[F, Byte, INothing] =
    ??? // _.pull

  def fromReadable[F[_]](readable: F[Readable])(implicit F: Async[F]): Stream[F, Byte] =
    Stream.resource(for {
      readable <- Resource.makeCase(readable) {
        case (readable, Resource.ExitCase.Succeeded) => F.delay(readable.destroy())
        case (readable, Resource.ExitCase.Errored(ex)) => F.delay(readable.destroy(js.Error(ex.getMessage())))
        case (readable, Resource.ExitCase.Canceled) => F.delay(readable.destroy())
      }
      dispatcher <- Dispatcher[F]
      queue <- Queue.synchronous[F, Unit].toResource
      ended <- Deferred[F, Either[Throwable, Unit]].toResource
      _ <- registerListener0(readable, nodeStrings.readable)(_.on_readable(_, _)) { () =>
        println("readable fired")
        dispatcher.unsafeRunAndForget(queue.offer(()))
      }
      _ <- registerListener0(readable, nodeStrings.end)(_.on_end(_, _)) { () =>
        dispatcher.unsafeRunAndForget(ended.complete(Right(())))
      }
      _ <- registerListener[js.Error](readable, nodeStrings.error)(_.on_error(_, _)) { e =>
        dispatcher.unsafeRunAndForget(ended.complete(Left(js.JavaScriptException(e))))
      }
    } yield (readable, queue, ended)).flatMap { case (readable, queue, ended) =>
      Stream.fromQueueUnterminated(queue).interruptWhen(ended) >>
        Stream.evalUnChunk(F.delay(Option(readable.read().asInstanceOf[Buffer]).fold(Chunk.empty[Byte])(_.toChunk)))
    }

}
