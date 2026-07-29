package com.yorel.muxon.console;

import com.yorel.muxon.db.model.ConsoleSessionEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.Socket;

/**
 * Opens a raw TCP/TLS connection to a VNC (RFB) endpoint on the hypervisor.
 * Byte-for-byte relay to the browser WebSocket is performed by {@link ConsoleWebSocketHandler}.
 */
@Service
public class VncProxyService {

    public Socket connect(ConsoleSessionEntity session, ConsoleProxyProperties properties) throws IOException {
        return HypervisorSocketFactory.connect(
                session.getHypervisorHost(),
                session.getHypervisorPort(),
                session.isTls(),
                properties.isTrustAllHypervisorTls());
    }
}
