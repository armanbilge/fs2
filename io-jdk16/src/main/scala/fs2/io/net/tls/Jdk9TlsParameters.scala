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

package fs2.io.net.tls

import java.util.function.BiFunction
import javax.net.ssl.{SSLEngine, SSLParameters}

private[tls] object Jdk9TlsParameters {
  def setEnableRetransmissions(p: SSLParameters, enableRetransmissions: Boolean): Unit =
    p.setEnableRetransmissions(enableRetransmissions)

  def setMaximumPacketSize(p: SSLParameters, maximumPacketSize: Int): Unit =
    p.setMaximumPacketSize(maximumPacketSize)

  def setApplicationProtocols(p: SSLParameters, protocols: Array[String]): Unit =
    p.setApplicationProtocols(protocols)

  def setHandshakeApplicationProtocolSelector(
      e: SSLEngine,
      handshakeApplicationProtocolSelector: (SSLEngine, List[String]) => String
  ): Unit = e.setHandshakeApplicationProtocolSelector(
    new BiFunction[SSLEngine, java.util.List[String], String] {
      def apply(engine: SSLEngine, protocols: java.util.List[String]): String =
        handshakeApplicationProtocolSelector(engine, List.tabulate(protocols.size)(protocols.get))
    }
  )
}
