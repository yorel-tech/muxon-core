package com.onetattva.infron.core.console;

import com.onetattva.infron.db.model.ConsoleSessionEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.Socket;

/**
 * Opens a raw TCP/TLS connection to a SPICE port on the hypervisor.
 * Relay semantics are identical to VNC at the transport layer for this proxy.
 */
@Service
public class SpiceProxyService {

    private final VncProxyService vncProxyService;

    public SpiceProxyService(VncProxyService vncProxyService) {
        this.vncProxyService = vncProxyService;
    }

    public Socket connect(ConsoleSessionEntity session, ConsoleProxyProperties properties) throws IOException {
        return vncProxyService.connect(session, properties);
    }
}
