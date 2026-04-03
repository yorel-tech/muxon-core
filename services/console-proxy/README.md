# console-proxy

Spring Boot service that authenticates VM console sessions (opaque token in the WebSocket URL) and relays **binary** traffic between the browser and the hypervisor VNC/SPICE port.

## Run locally

1. Apply DB migrations via **core-services** (or any service using Flyway on the same database) so `console_session` exists.
2. Start with the same PostgreSQL settings as other services.
3. Default HTTP/WebSocket port: **8082**; path: `/ws/console?token=...`

## Configuration

See `src/main/resources/application.yaml`:

- `infron.console.allowed-origins` — browser `Origin` allow list for WebSocket handshake.
- `infron.console.trust-all-hypervisor-tls` — dev-only; trust any certificate when connecting to Proxmox VNC proxy (TLS).
- `infron.console.cleanup-interval-ms` — how often to expire stale DB rows.

## Manual end-to-end check

1. Start **core-services**, **console-proxy**, and **infron-web**.
2. Ensure `infron.console.proxy-ws-base-url` in core-services matches the proxy URL (e.g. `ws://localhost:8082/ws/console`).
3. Create/start a VM; open **View console** in the tenant VMs UI.
