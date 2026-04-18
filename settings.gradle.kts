rootProject.name = "muxon-core"

include(
    "libs:core-api",
    "libs:core-persistence",
    "libs:core-proto",
    "libs:core-auth",
    "libs:core-commons",
    "libs:core-provider",
    "libs:core-worker",
    "services:auth-api",
    "services:muxon-initializer",
    "services:core-services",
    "services:orchestrator",
    "services:usage-billing",
    "services:console-proxy",
    "providers:mock",
    "providers:libvirt",
    "providers:proxmox",
    "integration-tests"
)
