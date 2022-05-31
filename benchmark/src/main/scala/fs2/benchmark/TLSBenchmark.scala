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
package benchmark

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.all._
import com.comcast.ip4s._
import fs2.io.net.Network
import fs2.io.net.Socket
import fs2.io.net.tls.TLSContext
import org.openjdk.jmh.annotations.{Benchmark, Param, Scope, Setup, State, TearDown}

@State(Scope.Thread)
class TLSBenchmark {

  @Param(Array("200", "2000", "20000"))
  var size: Int = _
  var msg: Chunk[Byte] = _
  var context: TLSContext[IO] = _
  var server: ((SocketAddress[IpAddress], Stream[IO, Socket[IO]]), IO[Unit]) = _

  @Setup
  def setup(): Unit = {
    msg = Chunk.array(("Hello, world! " * size).getBytes)
    context = Network[IO].tlsContext
      .fromKeyStoreResource(
        "keystore.jks",
        "password".toCharArray,
        "password".toCharArray
      )
      .unsafeRunSync()
    server = Network[IO]
      .serverResource(Some(ip"127.0.0.1"))
      .flatTap { case (_, sockets) =>
        sockets
          .map { socket =>
            socket.reads.chunks.foreach(socket.write(_))
          }
          .parJoinUnbounded
          .compile
          .drain
          .background
      }
      .allocated
      .unsafeRunSync()
  }

  @TearDown
  def tearDown(): Unit =
    server._2.unsafeRunSync()

  @Benchmark
  def echo(): Unit =
    Network[IO]
      .client(server._1._1)
      .flatMap(context.client(_))
      .use { socket =>
        (Stream.exec(socket.write(msg)).onFinalize(socket.endOfOutput) ++
          socket.reads.take(msg.size.toLong)).compile.drain
      }
      .unsafeRunSync()
}
