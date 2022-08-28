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

package fs2.io.net

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.syntax.all._
import fs2.Stream

import java.net.SocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.channels.spi.AsynchronousChannelProvider
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousCloseException

private[net] object AsyncChannelHelpers {

  def client[F[_]: Async](
      channelGroup: AsynchronousChannelGroup,
      to: F[SocketAddress],
      options: List[SocketOption]
  ): Resource[F, Socket[F]] = {
    def setup: Resource[F, AsynchronousSocketChannel] =
      Resource.make(Async[F].delay {
        val ch =
          AsynchronousChannelProvider.provider.openAsynchronousSocketChannel(channelGroup)
        options.foreach(opt => ch.setOption(opt.key, opt.value))
        ch
      })(ch => Async[F].delay(if (ch.isOpen) ch.close else ()))

    def connect(ch: AsynchronousSocketChannel): F[AsynchronousSocketChannel] =
      to.flatMap { ip =>
        Async[F].async_[AsynchronousSocketChannel] { cb =>
          ch.connect(
            ip,
            null,
            new CompletionHandler[Void, Void] {
              def completed(result: Void, attachment: Void): Unit =
                cb(Right(ch))
              def failed(rsn: Throwable, attachment: Void): Unit =
                cb(Left(rsn))
            }
          )
        }
      }

    setup.flatMap(ch => Resource.eval(connect(ch))).flatMap(Socket.forAsync(_))
  }

  def serverResource[F[_]: Async](
      channelGroup: AsynchronousChannelGroup,
      address: SocketAddress,
      options: List[SocketOption]
  ): Resource[F, (SocketAddress, Stream[F, Socket[F]])] = {

    val setup: F[AsynchronousServerSocketChannel] =
      Async[F].delay {
        val ch =
          AsynchronousChannelProvider.provider.openAsynchronousServerSocketChannel(channelGroup)
        ch.bind(address)
        ch
      }

    def cleanup(sch: AsynchronousServerSocketChannel): F[Unit] =
      Async[F].delay(if (sch.isOpen) sch.close())

    def acceptIncoming(
        sch: AsynchronousServerSocketChannel
    ): Stream[F, Socket[F]] = {
      def go: Stream[F, Socket[F]] = {
        def acceptChannel: F[AsynchronousSocketChannel] =
          Async[F].async_[AsynchronousSocketChannel] { cb =>
            sch.accept(
              null,
              new CompletionHandler[AsynchronousSocketChannel, Void] {
                def completed(ch: AsynchronousSocketChannel, attachment: Void): Unit =
                  cb(Right(ch))
                def failed(rsn: Throwable, attachment: Void): Unit =
                  cb(Left(rsn))
              }
            )
          }

        def setOpts(ch: AsynchronousSocketChannel) =
          Async[F].delay {
            options.foreach(o => ch.setOption(o.key, o.value))
          }

        Stream.eval(acceptChannel.attempt).flatMap {
          case Left(_) => Stream.empty[F]
          case Right(accepted) =>
            Stream.resource(Socket.forAsync(accepted).evalTap(_ => setOpts(accepted)))
        } ++ go
      }

      go.handleErrorWith {
        case err: AsynchronousCloseException =>
          Stream.eval(Async[F].delay(sch.isOpen)).flatMap { isOpen =>
            if (isOpen) Stream.raiseError[F](err)
            else Stream.empty
          }
        case err => Stream.raiseError[F](err)
      }
    }

    Resource.make(setup)(cleanup).map { sch =>
      (sch.getLocalAddress, acceptIncoming(sch))
    }
  }

}
