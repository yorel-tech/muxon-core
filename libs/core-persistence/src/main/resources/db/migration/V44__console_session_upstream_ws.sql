-- Optional Proxmox (and similar) upstream WebSocket for VNC relay instead of raw TCP to hypervisor:port

ALTER TABLE console_session
    ADD COLUMN IF NOT EXISTS upstream_ws_url TEXT,
    ADD COLUMN IF NOT EXISTS upstream_ws_cookie TEXT,
    ADD COLUMN IF NOT EXISTS upstream_ws_csrf TEXT,
    ADD COLUMN IF NOT EXISTS upstream_ws_authorization TEXT;
