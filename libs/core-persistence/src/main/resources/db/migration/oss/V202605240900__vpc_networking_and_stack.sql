-- VPC networking, stacks, and provider network layer
-- Covers: fabric_network, public_ip_pool, network_edge_node, datacenter_network_capabilities,
--         tenant_network_policy, stack, vpc, route_table, route_table_entry, subnet, subnet_rbac,
--         security_group, security_group_rule, floating_ip, internet_gateway, nat_gateway,
--         load_balancer family, ipam_prefix_delegation; plus nullable stack_id + subnet_id on vms.

-- ─── Enum types ────────────────────────────────────────────────────────────────

CREATE TYPE fabric_network_type AS ENUM ('BRIDGE', 'VLAN', 'VXLAN', 'OVS', 'UNDERLAY');
CREATE TYPE fabric_network_role AS ENUM ('TENANT_OVERLAY', 'STORAGE', 'MANAGEMENT', 'LIVE_MIGRATION');
CREATE TYPE fabric_network_status AS ENUM ('ACTIVE', 'INACTIVE');

CREATE TYPE network_edge_node_type AS ENUM ('STANDARD', 'HA_PRIMARY', 'HA_SECONDARY');
CREATE TYPE network_edge_node_status AS ENUM ('ACTIVE', 'MAINTENANCE', 'INACTIVE');

CREATE TYPE stack_status AS ENUM ('ACTIVE', 'INACTIVE', 'DELETING');

CREATE TYPE vpc_status AS ENUM ('ACTIVE', 'DELETING');

CREATE TYPE subnet_status AS ENUM ('PENDING', 'ACTIVE', 'ERROR', 'DELETING');

CREATE TYPE subnet_principal_type AS ENUM ('USER', 'STACK', 'ROLE');

CREATE TYPE route_target_type AS ENUM ('INTERNET_GATEWAY', 'NAT_GATEWAY', 'LOCAL', 'VPC_PEERING', 'INSTANCE');

CREATE TYPE sg_direction AS ENUM ('INGRESS', 'EGRESS');
CREATE TYPE sg_protocol AS ENUM ('TCP', 'UDP', 'ICMP', 'ALL');

CREATE TYPE floating_ip_status AS ENUM ('AVAILABLE', 'ASSOCIATED', 'RELEASING');

CREATE TYPE lb_scheme AS ENUM ('INTERNAL', 'INTERNET_FACING');

CREATE TYPE ipam_delegation_status AS ENUM ('PENDING', 'ACTIVE', 'ROUTE_ERROR', 'RELEASED');

-- ─── fabric_network ─────────────────────────────────────────────────────────────
-- Cluster-scoped, operator-managed underlay transport. Not tenant-visible.

CREATE TABLE fabric_network (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    node_cluster_id UUID NOT NULL REFERENCES node_clusters (id) ON DELETE RESTRICT,
    name            TEXT NOT NULL,
    type            fabric_network_type NOT NULL,
    role            fabric_network_role NOT NULL,
    external_id     TEXT,
    vlan_id         INTEGER,
    config          JSONB,
    status          fabric_network_status NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_fabric_network_cluster_name UNIQUE (node_cluster_id, name)
);

CREATE INDEX idx_fabric_network_cluster ON fabric_network (node_cluster_id);
CREATE INDEX idx_fabric_network_role ON fabric_network (role);

CREATE TRIGGER trg_fabric_network_updated
    BEFORE UPDATE ON fabric_network
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ─── public_ip_pool ─────────────────────────────────────────────────────────────

CREATE TABLE public_ip_pool (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    datacenter_id UUID NOT NULL REFERENCES datacenters (id) ON DELETE RESTRICT,
    cidr          TEXT NOT NULL,
    name          TEXT NOT NULL,
    description   TEXT,
    total_ips     INTEGER NOT NULL DEFAULT 0,
    allocated_ips INTEGER NOT NULL DEFAULT 0,
    status        TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_public_ip_pool_dc_cidr UNIQUE (datacenter_id, cidr)
);

CREATE INDEX idx_public_ip_pool_datacenter ON public_ip_pool (datacenter_id);

CREATE TRIGGER trg_public_ip_pool_updated
    BEFORE UPDATE ON public_ip_pool
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ─── network_edge_node ──────────────────────────────────────────────────────────

CREATE TABLE network_edge_node (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    datacenter_id UUID NOT NULL REFERENCES datacenters (id) ON DELETE RESTRICT,
    name          TEXT NOT NULL,
    host          TEXT NOT NULL,
    type          network_edge_node_type NOT NULL DEFAULT 'STANDARD',
    status        network_edge_node_status NOT NULL DEFAULT 'ACTIVE',
    capabilities  JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_network_edge_node_datacenter ON network_edge_node (datacenter_id);
CREATE INDEX idx_network_edge_node_status ON network_edge_node (status);

CREATE TRIGGER trg_network_edge_node_updated
    BEFORE UPDATE ON network_edge_node
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ─── datacenter_network_capabilities ────────────────────────────────────────────

CREATE TABLE datacenter_network_capabilities (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    datacenter_id         UUID NOT NULL REFERENCES datacenters (id) ON DELETE CASCADE,
    public_ip_supported   BOOLEAN NOT NULL DEFAULT false,
    vpn_supported         BOOLEAN NOT NULL DEFAULT false,
    bgp_supported         BOOLEAN NOT NULL DEFAULT false,
    ha_gateway_supported  BOOLEAN NOT NULL DEFAULT false,
    vxlan_supported       BOOLEAN NOT NULL DEFAULT false,
    multi_region_supported BOOLEAN NOT NULL DEFAULT false,
    l7_lb_supported       BOOLEAN NOT NULL DEFAULT false,
    ipv6_supported        BOOLEAN NOT NULL DEFAULT false,
    dual_stack_supported  BOOLEAN NOT NULL DEFAULT false,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_dc_network_caps_datacenter UNIQUE (datacenter_id)
);

CREATE TRIGGER trg_dc_network_caps_updated
    BEFORE UPDATE ON datacenter_network_capabilities
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ─── tenant_network_policy ───────────────────────────────────────────────────────

CREATE TABLE tenant_network_policy (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    max_vpcs              INTEGER NOT NULL DEFAULT 10,
    max_public_ips        INTEGER NOT NULL DEFAULT 5,
    max_subnets_per_vpc   INTEGER NOT NULL DEFAULT 10,
    vpn_allowed           BOOLEAN NOT NULL DEFAULT false,
    peering_allowed       BOOLEAN NOT NULL DEFAULT false,
    ha_networking_allowed BOOLEAN NOT NULL DEFAULT false,
    bandwidth_limit_mbps  INTEGER,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_tenant_network_policy_tenant UNIQUE (tenant_id)
);

CREATE TRIGGER trg_tenant_network_policy_updated
    BEFORE UPDATE ON tenant_network_policy
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ─── stack ──────────────────────────────────────────────────────────────────────

CREATE TABLE stack (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_datacenter_grant_id UUID NOT NULL REFERENCES tenant_datacenter_grants (id) ON DELETE RESTRICT,
    name                      TEXT NOT NULL,
    description               TEXT,
    status                    stack_status NOT NULL DEFAULT 'ACTIVE',
    metadata                  JSONB,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_stack_grant_name UNIQUE (tenant_datacenter_grant_id, name)
);

CREATE INDEX idx_stack_grant ON stack (tenant_datacenter_grant_id);
CREATE INDEX idx_stack_status ON stack (status);

CREATE TRIGGER trg_stack_updated
    BEFORE UPDATE ON stack
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ─── vpc ────────────────────────────────────────────────────────────────────────
-- Tenant-global — FK to tenant, NOT tenant_datacenter_grant.

CREATE TABLE vpc (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants (id) ON DELETE RESTRICT,
    name        TEXT NOT NULL,
    cidr        TEXT NOT NULL,
    cidr_v6     TEXT,
    description TEXT,
    region_id   UUID,
    status      vpc_status NOT NULL DEFAULT 'ACTIVE',
    metadata    JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_vpc_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_vpc_tenant ON vpc (tenant_id);
CREATE INDEX idx_vpc_status ON vpc (status);

CREATE TRIGGER trg_vpc_updated
    BEFORE UPDATE ON vpc
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ─── route_table (before subnet, because subnet has FK → route_table) ────────────

CREATE TABLE route_table (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vpc_id     UUID NOT NULL REFERENCES vpc (id) ON DELETE CASCADE,
    name       TEXT NOT NULL,
    is_main    BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_route_table_vpc ON route_table (vpc_id);

CREATE TRIGGER trg_route_table_updated
    BEFORE UPDATE ON route_table
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

CREATE TABLE route_table_entry (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_table_id   UUID NOT NULL REFERENCES route_table (id) ON DELETE CASCADE,
    destination_cidr TEXT NOT NULL,
    target_type      route_target_type NOT NULL,
    target_id        UUID,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_route_table_entry_table ON route_table_entry (route_table_id);

-- ─── subnet ─────────────────────────────────────────────────────────────────────

CREATE TABLE subnet (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vpc_id            UUID NOT NULL REFERENCES vpc (id) ON DELETE RESTRICT,
    datacenter_id     UUID NOT NULL REFERENCES datacenters (id) ON DELETE RESTRICT,
    fabric_network_id UUID REFERENCES fabric_network (id) ON DELETE SET NULL,
    name              TEXT NOT NULL,
    cidr              TEXT NOT NULL,
    cidr_v6           TEXT,
    gateway_ip        TEXT,
    dns_servers       TEXT[],
    public            BOOLEAN NOT NULL DEFAULT false,
    availability_zone TEXT,
    route_table_id    UUID REFERENCES route_table (id) ON DELETE SET NULL,
    status            subnet_status NOT NULL DEFAULT 'PENDING',
    provider_handle   TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_subnet_vpc ON subnet (vpc_id);
CREATE INDEX idx_subnet_datacenter ON subnet (datacenter_id);
CREATE INDEX idx_subnet_fabric_network ON subnet (fabric_network_id);
CREATE INDEX idx_subnet_status ON subnet (status);

CREATE TRIGGER trg_subnet_updated
    BEFORE UPDATE ON subnet
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ─── subnet_rbac ────────────────────────────────────────────────────────────────

CREATE TABLE subnet_rbac (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subnet_id      UUID NOT NULL REFERENCES subnet (id) ON DELETE CASCADE,
    principal_type subnet_principal_type NOT NULL,
    principal_id   UUID NOT NULL,
    permissions    TEXT[] NOT NULL DEFAULT '{}',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_subnet_rbac_principal UNIQUE (subnet_id, principal_type, principal_id)
);

CREATE INDEX idx_subnet_rbac_subnet ON subnet_rbac (subnet_id);
CREATE INDEX idx_subnet_rbac_principal ON subnet_rbac (subnet_id, principal_id);

CREATE TRIGGER trg_subnet_rbac_updated
    BEFORE UPDATE ON subnet_rbac
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ─── security_group ─────────────────────────────────────────────────────────────

CREATE TABLE security_group (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vpc_id      UUID NOT NULL REFERENCES vpc (id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_security_group_vpc ON security_group (vpc_id);

CREATE TRIGGER trg_security_group_updated
    BEFORE UPDATE ON security_group
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

CREATE TABLE security_group_rule (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    security_group_id       UUID NOT NULL REFERENCES security_group (id) ON DELETE CASCADE,
    direction               sg_direction NOT NULL,
    protocol                sg_protocol NOT NULL DEFAULT 'ALL',
    port_range_min          INTEGER,
    port_range_max          INTEGER,
    cidr                    TEXT,
    source_security_group_id UUID REFERENCES security_group (id) ON DELETE SET NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_sg_rule_group ON security_group_rule (security_group_id);

-- ─── floating_ip ────────────────────────────────────────────────────────────────

CREATE TABLE floating_ip (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants (id) ON DELETE RESTRICT,
    datacenter_id       UUID NOT NULL REFERENCES datacenters (id) ON DELETE RESTRICT,
    public_ip_pool_id   UUID NOT NULL REFERENCES public_ip_pool (id) ON DELETE RESTRICT,
    ip_address          TEXT NOT NULL,
    ip_address_v6       TEXT,
    associated_vm_id    UUID REFERENCES vms (id) ON DELETE SET NULL,
    associated_private_ip TEXT,
    status              floating_ip_status NOT NULL DEFAULT 'AVAILABLE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_floating_ip_tenant ON floating_ip (tenant_id);
CREATE INDEX idx_floating_ip_datacenter ON floating_ip (datacenter_id);
CREATE INDEX idx_floating_ip_pool ON floating_ip (public_ip_pool_id);
CREATE INDEX idx_floating_ip_vm ON floating_ip (associated_vm_id);

CREATE TRIGGER trg_floating_ip_updated
    BEFORE UPDATE ON floating_ip
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ─── internet_gateway ───────────────────────────────────────────────────────────

CREATE TABLE internet_gateway (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vpc_id               UUID NOT NULL REFERENCES vpc (id) ON DELETE CASCADE,
    network_edge_node_id UUID REFERENCES network_edge_node (id) ON DELETE SET NULL,
    name                 TEXT NOT NULL,
    status               TEXT NOT NULL DEFAULT 'PENDING',
    ha_mode              TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_internet_gateway_vpc UNIQUE (vpc_id)
);

CREATE TRIGGER trg_internet_gateway_updated
    BEFORE UPDATE ON internet_gateway
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ─── nat_gateway ────────────────────────────────────────────────────────────────

CREATE TABLE nat_gateway (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vpc_id               UUID NOT NULL REFERENCES vpc (id) ON DELETE CASCADE,
    subnet_id            UUID NOT NULL REFERENCES subnet (id) ON DELETE RESTRICT,
    network_edge_node_id UUID REFERENCES network_edge_node (id) ON DELETE SET NULL,
    name                 TEXT NOT NULL,
    floating_ip_id       UUID REFERENCES floating_ip (id) ON DELETE SET NULL,
    status               TEXT NOT NULL DEFAULT 'PENDING',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_nat_gateway_vpc ON nat_gateway (vpc_id);

CREATE TRIGGER trg_nat_gateway_updated
    BEFORE UPDATE ON nat_gateway
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ─── load_balancer ──────────────────────────────────────────────────────────────

CREATE TABLE load_balancer (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vpc_id         UUID NOT NULL REFERENCES vpc (id) ON DELETE CASCADE,
    subnet_id      UUID NOT NULL REFERENCES subnet (id) ON DELETE RESTRICT,
    name           TEXT NOT NULL,
    scheme         lb_scheme NOT NULL DEFAULT 'INTERNAL',
    floating_ip_id UUID REFERENCES floating_ip (id) ON DELETE SET NULL,
    status         TEXT NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_load_balancer_vpc ON load_balancer (vpc_id);

CREATE TRIGGER trg_load_balancer_updated
    BEFORE UPDATE ON load_balancer
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

CREATE TABLE load_balancer_listener (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    load_balancer_id UUID NOT NULL REFERENCES load_balancer (id) ON DELETE CASCADE,
    protocol         TEXT NOT NULL DEFAULT 'TCP',
    port             INTEGER NOT NULL,
    target_port      INTEGER NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_lb_listener_lb ON load_balancer_listener (load_balancer_id);

CREATE TRIGGER trg_lb_listener_updated
    BEFORE UPDATE ON load_balancer_listener
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

CREATE TABLE load_balancer_target (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    load_balancer_id UUID NOT NULL REFERENCES load_balancer (id) ON DELETE CASCADE,
    vm_id            UUID REFERENCES vms (id) ON DELETE SET NULL,
    ip_address       TEXT,
    port             INTEGER NOT NULL,
    weight           INTEGER NOT NULL DEFAULT 1,
    status           TEXT NOT NULL DEFAULT 'HEALTHY',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_lb_target_lb ON load_balancer_target (load_balancer_id);

CREATE TRIGGER trg_lb_target_updated
    BEFORE UPDATE ON load_balancer_target
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ─── ipam_prefix_delegation ─────────────────────────────────────────────────────

CREATE TABLE ipam_prefix_delegation (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subnet_id      UUID NOT NULL REFERENCES subnet (id) ON DELETE RESTRICT,
    stack_id       UUID NOT NULL REFERENCES stack (id) ON DELETE RESTRICT,
    node_vm_id     UUID NOT NULL REFERENCES vms (id) ON DELETE RESTRICT,
    delegated_cidr TEXT NOT NULL,
    status         ipam_delegation_status NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ipam_delegation_subnet ON ipam_prefix_delegation (subnet_id);
CREATE INDEX idx_ipam_delegation_stack ON ipam_prefix_delegation (stack_id);
CREATE INDEX idx_ipam_delegation_status ON ipam_prefix_delegation (status);

CREATE TRIGGER trg_ipam_delegation_updated
    BEFORE UPDATE ON ipam_prefix_delegation
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ─── vm additions ────────────────────────────────────────────────────────────────

ALTER TABLE vms
    ADD COLUMN stack_id  UUID REFERENCES stack (id) ON DELETE SET NULL,
    ADD COLUMN subnet_id UUID REFERENCES subnet (id) ON DELETE SET NULL;

CREATE INDEX idx_vm_stack ON vms (stack_id);
CREATE INDEX idx_vm_subnet ON vms (subnet_id);
