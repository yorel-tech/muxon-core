package com.onetattva.infron.core.providers;

/**
 * TCP target and credentials for the console proxy to connect to the hypervisor.
 *
 * @param consoleType Type of stream (VNC binary RFB, SPICE, etc.)
 * @param host        Resolvable host or IP of the hypervisor / node
 * @param port        TCP port for the console service
 * @param password    VNC password / Proxmox ticket (may be null)
 * @param tls         Whether the TCP connection should use TLS (e.g. Proxmox VNC proxy)
 * @param tlsPort     Optional explicit TLS port if different from {@code port}
 */
public record VmConsoleConnectionInfo(
        VmConsoleType consoleType,
        String host,
        int port,
        String password,
        boolean tls,
        Integer tlsPort
) {
}
