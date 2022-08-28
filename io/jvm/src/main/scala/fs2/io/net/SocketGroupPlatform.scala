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
package net

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.syntax.all._
import com.comcast.ip4s.Host
import com.comcast.ip4s.IpAddress
import com.comcast.ip4s.Port
import com.comcast.ip4s.SocketAddress

import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup

private[net] trait SocketGroupCompanionPlatform { self: SocketGroup.type =>
  private[fs2] def unsafe[F[_]: Async](channelGroup: AsynchronousChannelGroup): SocketGroup[F] =
    new AsyncSocketGroup[F](channelGroup)

  private final class AsyncSocketGroup[F[_]: Async](channelGroup: AsynchronousChannelGroup)
      extends AbstractAsyncSocketGroup[F] {

    def client(
        to: SocketAddress[Host],
        options: List[SocketOption]
    ): Resource[F, Socket[F]] =
      AsyncChannelHelpers.client(channelGroup, to.resolve[F].map(_.toInetSocketAddress), options)

    def serverResource(
        address: Option[Host],
        port: Option[Port],
        options: List[SocketOption]
    ): Resource[F, (SocketAddress[IpAddress], Stream[F, Socket[F]])] =
      Resource.eval(address.traverse(_.resolve[F])).flatMap { addr =>
        AsyncChannelHelpers
          .serverResource(
            channelGroup,
            new InetSocketAddress(
              addr.map(_.toInetAddress).orNull,
              port.map(_.value).getOrElse(0)
            ),
            options
          )
          .map { case (jLocalAddress, incoming) =>
            val localAddress = SocketAddress
              .fromInetSocketAddress(jLocalAddress.asInstanceOf[java.net.InetSocketAddress])
            (localAddress, incoming)
          }
      }

  }

}
