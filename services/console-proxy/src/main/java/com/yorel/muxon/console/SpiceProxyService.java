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

import com.yorel.muxon.db.model.ConsoleSessionEntity;
import java.io.IOException;
import java.net.Socket;
import org.springframework.stereotype.Service;

/**
 * Opens a raw TCP/TLS connection to a SPICE port on the hypervisor. Relay semantics are identical
 * to VNC at the transport layer for this proxy.
 */
@Service
public class SpiceProxyService {

  private final VncProxyService vncProxyService;

  public SpiceProxyService(VncProxyService vncProxyService) {
    this.vncProxyService = vncProxyService;
  }

  public Socket connect(ConsoleSessionEntity session, ConsoleProxyProperties properties)
      throws IOException {
    return vncProxyService.connect(session, properties);
  }
}
