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
package io.net

import cats.effect.FileDescriptorPollHandle
import cats.effect.IO
import cats.effect.LiftIO
import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import fs2.io.internal.NativeUtil._
import fs2.io.internal.SocketHelpers

import scala.scalanative.posix.sys.socket._
import scala.scalanative.unsafe._
import scala.scalanative.unsigned._

import FdPollingDatagramSocket._

private final class FdPollingDatagramSocket[F[_]: LiftIO] private (
    fd: Int,
    handle: FileDescriptorPollHandle,
    buffer: Ptr[Byte],
    ipv4: Boolean
)(implicit F: Async[F])
    extends DatagramSocket[F] {

  def read = handle
    .pollReadRec(()) { _ =>
      IO {
        SocketHelpers.allocateSockaddr { (src_addr, addrlen) =>
          val rtn = guardSSize(recvfrom(fd, buffer, BufferSize.toULong, 0, src_addr, addrlen))
          if (rtn >= 0) {
            val remote = SocketHelpers.toSocketAddress(src_addr, ipv4)
            val bytes = Chunk.fromBytePtr(buffer, rtn.toInt)
            Right(Datagram(remote, bytes))
          } else Left(())
        }
      }
    }
    .to

  def reads = Stream.repeatEval(read)

  def write(datagram: Datagram) = handle
    .pollWriteRec(()) { _ =>
      IO {
        SocketHelpers.toSockaddr(datagram.remote) { (dest_addr, addrlen) =>
          val Chunk.ArraySlice(buf, offset, length) = datagram.bytes.toArraySlice
          val rtn = guardSSize(sendto(fd, buf.at(offset), length.toULong, 0, dest_addr, addrlen))
          if (rtn >= 0) Right(()) else Left(())
        }
      }
    }
    .to

  def writes = _.foreach(write)

  def localAddress = SocketHelpers.getLocalAddress(fd, ipv4)

}

private object FdPollingDatagramSocket {
  final val BufferSize = 1 << 16

  def apply[F[_]: Async: LiftIO](
      fd: Int,
      handle: FileDescriptorPollHandle,
      ipv4: Boolean
  ): Resource[F, FdPollingDatagramSocket[F]] =
    malloc(BufferSize.toULong).map(new FdPollingDatagramSocket(fd, handle, _, ipv4))
}
