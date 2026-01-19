rootProject.name = "infron-core"

include(
    "libs:core-api",
    "libs:core-persistence",
    "libs:core-proto",
    "libs:core-auth",
    "libs:core-spi",
    "libs:core-commons",
    "libs:core-provider",
    "services:auth-api",
    "services:bootstrap-initializer",
    "services:core-services",
    "services:orchestrator",
    "services:usage-billing",
    "services:agent",
    "services:console-proxy",
    "providers:mock",
    "providers:libvirt",
    "providers:proxmox",
    "integration-tests"
)
