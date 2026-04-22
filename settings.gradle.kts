rootProject.name = "muxon-core"

include(
    "libs:core-api",
    "libs:core-persistence",
    "libs:core-proto",
    "libs:core-auth",
    "libs:core-commons",
    "libs:core-customization",
    "libs:core-initializer",
    "libs:core-provider",
    "libs:core-worker",
    "services:auth-api",
    "services:muxon-initializer",
    "services:core-services",
    "services:orchestrator",
    "services:console-proxy",
    "providers:mock",
    "providers:libvirt",
    "providers:proxmox",
    "integration-tests"
)
