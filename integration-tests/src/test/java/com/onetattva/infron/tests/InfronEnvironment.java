package com.onetattva.infron.tests;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;
import org.opentest4j.TestAbortedException;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class InfronEnvironment {

    // Singleton instance
    private static volatile InfronEnvironment instance;
    private static final Object lock = new Object();

    // Infrastructure state
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private CompletableFuture<Void> initializationFuture;

    // Docker and container constants
    private static final String PODMAN_SOCKET_PATH = "unix:///run/user/1000/podman/podman.sock";
    private static final String POSTGRES_IMAGE = "postgres:18";
    private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.4.0";

    // Network aliases
    private static final String POSTGRES_ALIAS = "postgres";
    private static final String KEYCLOAK_ALIAS = "keycloak";
    private static final String CORE_SERVICES_ALIAS = "core-services";

    // Database constants
    private static final String DB_NAME = "infron";
    private static final String DB_USERNAME = "infron";
    public static final String DB_PASSWORD = "Infr0n@1234";

    // File paths
    private static final String KEYCLOAK_REALM_PATH = "../compose/keycloak/realm-infron-dev.json";
    private static final String CONTAINER_REALM_PATH = "/opt/keycloak/data/import/infron-dev.json";
    private static final String PASSPHRASE_FILE_PATH = "/run/secrets/infron-passphrase";
    private static final String OIDC_SECRET_FILE_PATH = "/run/secrets/infron-oidc-secret";
    private static final String DB_PASSWORD_FILE_PATH = "/run/secrets/infron-db-password";
    private static final String INITIAL_CONFIG_PATH = "src/test/resources/initial-config.yaml";
    private static final String CONTAINER_CONFIG_PATH = "/etc/infron/initial-config.yaml";

    // Image names
    private static final String CORE_SERVICES_IMAGE = "infron-core-services:test";

    // Keycloak commands
    private static final String KEYCLOAK_START_CMD = "start-dev";
    private static final String KEYCLOAK_HTTP_PORT = "--http-port=8085";
    private static final String KEYCLOAK_IMPORT_REALM = "--import-realm";
    private static final String KEYCLOAK_ADMIN_USER = "admin";

    // Spring configuration
    private static final String SPRING_CONFIG_LOCATION = "file:/etc/infron/";

    // Containers (private for encapsulation)
    private Network network;
    private PostgreSQLContainer<?> postgres;
    private GenericContainer<?> keycloak;
    private GenericContainer<?> coreServices;

    private InfronEnvironment() {
        // Private constructor for singleton
    }

    public static InfronEnvironment getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new InfronEnvironment();
                }
            }
        }
        return instance;
    }

    public synchronized void startInfrastructure() throws Exception {
        if (!DockerClientFactory.instance().isDockerAvailable()) {
            throw new TestAbortedException("Docker is not available; skipping container-based integration tests");
        }
        if (initialized.get()) {
            return; // Already initialized
        }

        if (initializationFuture != null && !initializationFuture.isDone()) {
            // Wait for ongoing initialization
            initializationFuture.join();
            return;
        }

        initializationFuture = CompletableFuture.runAsync(() -> {
            try {
                initializeInfrastructure();
                initialized.set(true);
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize infrastructure", e);
            }
        });

        // Wait for completion
        initializationFuture.join();
    }

    private void initializeInfrastructure() throws Exception {
        // Create a shared network for all containers (pod-like behavior)
        network = Network.newNetwork();

        startPostgres();
        startKeycloak();
        startCoreServices();
    }

    private void startPostgres() {
        System.out.println("Starting infron-its-postgres...");
        postgres = new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
                .withDatabaseName(DB_NAME)
                .withUsername(DB_USERNAME)
                .withPassword(DB_PASSWORD)
                .withNetwork(network)
                .withNetworkAliases(POSTGRES_ALIAS)
                .withCreateContainerCmdModifier(cmd -> cmd.withName("infron-its-postgres"))
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 1).withStartupTimeout(java.time.Duration.ofMinutes(2)));
        try {
            postgres.start();
            System.out.println("infron-its-postgres started.");
        } catch (Exception e) {
            System.out.println("Failed to start infron-its-postgres. Logs:");
            System.out.println(postgres.getLogs());
            throw e;
        }
    }

    private void startKeycloak() {
        System.out.println("Starting infron-its-keycloak...");
        keycloak = new GenericContainer<>(DockerImageName.parse(KEYCLOAK_IMAGE))
                .withCommand(KEYCLOAK_START_CMD, KEYCLOAK_HTTP_PORT, KEYCLOAK_IMPORT_REALM)
                .withEnv(Map.of(
                        "KEYCLOAK_ADMIN", KEYCLOAK_ADMIN_USER,
                        "KEYCLOAK_ADMIN_PASSWORD", DB_PASSWORD,
                        "KC_HEALTH_ENABLED", "true",
                        "KC_HOSTNAME", "keycloak",
                        "KC_HOSTNAME_PORT", "8085",
                        "KC_HOSTNAME_STRICT", "false",
                        "KC_HOSTNAME_STRICT_BACKCHANNEL", "false",
                        "INFRON_API_CLIENT_SECRET", "yOLpyss3IYlm2MadOdqAIfCpQne62Jdd"
                ))
                .withExposedPorts(8085)
                .withNetwork(network)
                .withNetworkAliases(KEYCLOAK_ALIAS)
                .withCreateContainerCmdModifier(cmd -> cmd.withName("infron-its-keycloak"))
                .withFileSystemBind(KEYCLOAK_REALM_PATH, CONTAINER_REALM_PATH, org.testcontainers.containers.BindMode.READ_ONLY)
                .waitingFor(Wait.forLogMessage(".*Listening on.*", 1).withStartupTimeout(java.time.Duration.ofMinutes(5)));
        try {
            keycloak.start();
            System.out.println("infron-its-keycloak started.");
        } catch (Exception e) {
            System.out.println("Failed to start infron-its-keycloak. Logs:");
            System.out.println(keycloak.getLogs());
            throw e;
        }
    }

    private void startCoreServices() throws IOException {
        System.out.println("Starting infron-its-core-services...");
        // Create a temporary file with the passphrase for podman secret simulation
        Path passphraseFile = Files.createTempFile("passphrase", ".txt");
        Files.writeString(passphraseFile, DB_PASSWORD);

        // Create a temporary file with the OIDC secret
        Path oidcSecretFile = Files.createTempFile("oidc-secret", ".txt");
        Files.writeString(oidcSecretFile, "yOLpyss3IYlm2MadOdqAIfCpQne62Jdd");

        // Create a temporary file with the DB password
        Path dbPasswordFile = Files.createTempFile("db-password", ".txt");
        Files.writeString(dbPasswordFile, DB_PASSWORD);

        coreServices = new GenericContainer<>(DockerImageName.parse(CORE_SERVICES_IMAGE))
            .withExposedPorts(8080)
            .withNetwork(network)
            .withNetworkAliases(CORE_SERVICES_ALIAS)
            .withCreateContainerCmdModifier(cmd -> cmd.withName("infron-its-core-services"))
            .withEnv(Map.of(
                "SPRING_CONFIG_LOCATION", SPRING_CONFIG_LOCATION
            ))
            .withFileSystemBind(INITIAL_CONFIG_PATH, CONTAINER_CONFIG_PATH, org.testcontainers.containers.BindMode.READ_ONLY)
            .withFileSystemBind(passphraseFile.toString(), PASSPHRASE_FILE_PATH, org.testcontainers.containers.BindMode.READ_ONLY)
            .withFileSystemBind(oidcSecretFile.toString(), OIDC_SECRET_FILE_PATH, org.testcontainers.containers.BindMode.READ_ONLY)
            .withFileSystemBind(dbPasswordFile.toString(), DB_PASSWORD_FILE_PATH, org.testcontainers.containers.BindMode.READ_ONLY)
            .waitingFor(Wait.forLogMessage(".*Started CoreServicesApplication.*", 1).withStartupTimeout(java.time.Duration.ofMinutes(5)));
        try {
            coreServices.start();
            System.out.println("infron-its-core-services started.");

            // Install curl for testing purposes
            try {
                Container.ExecResult installResult = coreServices.execInContainer("apk", "add", "--no-cache", "curl");
                if (installResult.getExitCode() != 0) {
                    System.out.println("Failed to install curl: " + installResult.getStderr());
                } else {
                    System.out.println("curl installed successfully in core-services container.");
                }
            } catch (Exception e) {
                System.out.println("Exception installing curl: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Failed to start infron-its-core-services. Logs:");
            System.out.println(coreServices.getLogs());
            throw e;
        }
    }

    public void waitForInitialization() {
        if (!initialized.get()) {
            if (initializationFuture != null) {
                initializationFuture.join(); // Wait for initialization to complete
            } else {
                throw new IllegalStateException("Infrastructure has not been started. Call startInfrastructure() first.");
            }
        }
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public String getCoreServicesUrl() {
        waitForInitialization();
        return "http://" + coreServices.getHost() + ":" + coreServices.getMappedPort(8080) + "/api/v1";
    }

    public String getKeycloakUrl() {
        waitForInitialization();
        return getKeycloakUrlWithoutInitialization();
    }

    private @NotNull String getKeycloakUrlWithoutInitialization() {
        return "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8085);
    }

    public boolean isPostgresRunning() {
        waitForInitialization();
        return postgres != null && postgres.isRunning();
    }

    public boolean isKeycloakRunning() {
        waitForInitialization();
        return keycloak != null && keycloak.isRunning();
    }

    public boolean isCoreServicesRunning() {
        waitForInitialization();
        return coreServices != null && coreServices.isRunning();
    }

    public GenericContainer<?> getCoreServices() {
        waitForInitialization();
        return coreServices;
    }

    public void collectCoreServicesLogs() throws IOException {
        if (coreServices == null) {
            return;
        }
        Path logsDir = Paths.get("logs");
        Files.createDirectories(logsDir);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String logFileName = "core-services.log";
        Path logFile = logsDir.resolve(logFileName);
        String logs = coreServices.getLogs();
        Files.writeString(logFile, logs);
        String zipFileName = "core-services-logs-" + timestamp + ".zip";
        Path zipFile = logsDir.resolve(zipFileName);
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipFile)))) {
            ZipEntry entry = new ZipEntry(logFileName);
            zos.putNextEntry(entry);
            zos.write(logs.getBytes());
            zos.closeEntry();
        }
        System.out.println("Core services logs collected and saved to " + zipFile.toString());
    }

    public void stopInfrastructure() {
        if (coreServices != null) {
            coreServices.stop();
        }
        if (keycloak != null) {
            keycloak.stop();
        }
        if (postgres != null) {
            postgres.stop();
        }
        if (network != null) {
            network.close();
        }
        initialized.set(false);
    }
}
