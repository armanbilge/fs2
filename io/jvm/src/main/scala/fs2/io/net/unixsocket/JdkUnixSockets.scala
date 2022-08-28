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

package fs2.io.net.unixsocket

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import fs2.Stream
import fs2.io.net.AsyncChannelHelpers
import fs2.io.net.Network
import fs2.io.net.Socket

import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.AsynchronousChannelGroup

object JdkUnixSockets {

  def supported: Boolean = StandardProtocolFamily.values.size > 2

  implicit def forAsync[F[_]: Async]: UnixSockets[F] =
    new JdkUnixSocketsImpl[F](Network.globalAcg)
}

private[unixsocket] class JdkUnixSocketsImpl[F[_]](channelGroup: AsynchronousChannelGroup)(implicit
    F: Async[F]
) extends UnixSockets[F] {

  override def client(address: UnixSocketAddress): Resource[F, Socket[F]] =
    AsyncChannelHelpers.client(channelGroup, F.delay(UnixDomainSocketAddress.of(address.path)), Nil)

  override def server(
      address: UnixSocketAddress,
      deleteIfExists: Boolean,
      deleteOnClose: Boolean
  ): fs2.Stream[F, Socket[F]] =
    Stream
      .resource(
        AsyncChannelHelpers
          .serverResource(channelGroup, UnixDomainSocketAddress.of(address.path), Nil)
      )
      .flatMap(_._2)

}
