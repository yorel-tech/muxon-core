rootProject.name = "infron-core"

include(
    "libs:core-api",
    "libs:core-persistence",
    "libs:core-proto",
    "libs:core-auth",
    "libs:core-spi",
    "libs:core-commons",
    "services:api-gateway",
    "services:core-services",
    "services:orchestrator",
    "services:usage-billing",
    "services:agent",
    "services:console-proxy",
    "providers:vsphere",
    "providers:libvirt",
    "providers:proxmox"
)
