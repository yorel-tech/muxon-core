/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.console;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/** Opens TCP or TLS sockets to hypervisor console ports. */
public final class HypervisorSocketFactory {

  private HypervisorSocketFactory() {}

  public static Socket connect(String host, int port, boolean tls, boolean trustAllTls)
      throws IOException {
    if (!tls) {
      Socket s = new Socket();
      s.connect(new InetSocketAddress(host, port), 15_000);
      s.setTcpNoDelay(true);
      return s;
    }
    SSLSocketFactory factory =
        trustAllTls ? insecureSslSocketFactory() : (SSLSocketFactory) SSLSocketFactory.getDefault();
    SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
    socket.setTcpNoDelay(true);
    socket.startHandshake();
    return socket;
  }

  private static SSLSocketFactory insecureSslSocketFactory() {
    try {
      TrustManager[] trustAll =
          new TrustManager[] {
            new X509TrustManager() {
              @Override
              public void checkClientTrusted(X509Certificate[] chain, String authType) {}

              @Override
              public void checkServerTrusted(X509Certificate[] chain, String authType) {}

              @Override
              public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
              }
            }
          };
      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(null, trustAll, new SecureRandom());
      return ctx.getSocketFactory();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to build insecure SSL socket factory", e);
    }
  }
}
