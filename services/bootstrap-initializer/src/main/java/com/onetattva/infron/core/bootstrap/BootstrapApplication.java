package com.onetattva.infron.core.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.onetattva.infron.core.common.Constants;
import com.onetattva.infron.core.common.EncryptionUtil;
import com.onetattva.infron.db.RoleBindingSubjectType;
import com.onetattva.infron.db.RoleScopeType;
import com.onetattva.infron.db.TenantStatus;
import com.onetattva.infron.db.model.*;
import com.onetattva.infron.db.repository.*;
import com.onetattva.infron.core.services.OidcUserService;
import com.onetattva.infron.core.services.model.OidcUserInfo;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;

import static com.onetattva.infron.core.common.Constants.BOOTSTRAP_DONE_KEY;

@SpringBootApplication
@EntityScan(basePackages = "com.onetattva.infron.db.model")
@EnableJpaRepositories(basePackages = "com.onetattva.infron.db.repository")
public class BootstrapApplication implements CommandLineRunner {

    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory())
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    private IdentityProviderRepository identityProviderRepository;

    @Autowired
    private IdpUserRepository idpUserRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoleBindingRepository roleBindingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public static void main(String[] args) {
        String configPath = null;
        String outputFolder = null;
        String passphrasePath = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--initial-config":
                    if (i + 1 < args.length) {
                        configPath = args[++i];
                    } else {
                        System.out.println("Missing value for --initial-config");
                        printUsageAndExit();
                    }
                    break;
                case "--output-folder":
                    if (i + 1 < args.length) {
                        outputFolder = args[++i];
                    } else {
                        System.out.println("Missing value for --output-folder");
                        printUsageAndExit();
                    }
                    break;
                case "--pass-phrase":
                    if (i + 1 < args.length) {
                        passphrasePath = args[++i];
                    } else {
                        System.out.println("Missing value for --pass-phrase");
                        printUsageAndExit();
                    }
                    break;
                default:
                    System.out.println("Unknown argument: " + args[i]);
                    printUsageAndExit();
            }
        }

        if (configPath == null || outputFolder == null || passphrasePath == null) {
            System.out.println("All arguments are required:");
            printUsageAndExit();
        }

        // Start Spring Boot application with arguments
        SpringApplication app = new SpringApplication(BootstrapApplication.class);
        app.run("--configPath=" + configPath, "--outputFolder=" + outputFolder, "--passphrasePath=" + passphrasePath);
    }

    private static void printUsageAndExit() {
        System.out.println("Usage: java -jar bootstrap-initializer.jar --initial-config <path> --output-folder <path> --pass-phrase <phrase>");
        System.exit(1);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting bootstrap initialization");

        // Parse arguments from the command line args passed to run
        String configPath = null;
        String outputFolder = null;
        String passphrasePath = null;

        for (String arg : args) {
            if (arg.startsWith("--configPath=")) {
                configPath = arg.substring("--configPath=".length());
            } else if (arg.startsWith("--outputFolder=")) {
                outputFolder = arg.substring("--outputFolder=".length());
            } else if (arg.startsWith("--passphrasePath=")) {
                passphrasePath = arg.substring("--passphrasePath=".length());
            }
        }

        if (configPath == null || outputFolder == null || passphrasePath == null) {
            throw new IllegalArgumentException("Missing required arguments: configPath, outputFolder, passphrasePath");
        }

        boolean lockAcquired = false;
        try {
            lockAcquired = acquireLock();
            if (!lockAcquired) {
                throw new RuntimeException("Failed to acquire bootstrap advisory lock within timeout");
            }

            String bootstrapValue = getBootstrapValue();
            if ("true".equals(bootstrapValue)) {
                System.out.println("Bootstrap already completed");
                return;
            }

            performInitialization(configPath, outputFolder, passphrasePath);

            jdbcTemplate.update("INSERT INTO system_init (primary_key, value, updated_at) VALUES (?, ?, now()) ON CONFLICT (primary_key) DO UPDATE SET value = EXCLUDED.value, updated_at = now()", BOOTSTRAP_DONE_KEY, "true");
            System.out.println("Bootstrap initialization completed");

        } catch (Exception e) {
            System.err.println("Error during bootstrap initialization:" + e.getMessage());
            throw e;
        } finally {
            if (lockAcquired) {
                releaseLock();
            }
        }
        System.exit(0);
    }

    public void performInitialization(String configPath, String outputFolder, String passphrasePath) throws Exception {
        try {
            // Load initial config
            BootstrapConfig config = loadConfig(configPath);

            // Read passphrase from file
            String passphrase = Files.readString(Paths.get(passphrasePath)).trim();

            // Generate encryption key from instance details and passphrase
            String encryptionKey = generateEncryptionKey(config.getInfron().getInstanceName(),
                    config.getInfron().getInstanceId(),
                    passphrase);
            EncryptionUtil.setKey(encryptionKey);

            // Generate application.yaml with encrypted password
            generateApplicationYaml(config, outputFolder, encryptionKey);

            // Run flyway migrations
            runFlywayMigrations(config);

            // Insert bootstrap data using repositories
            insertBootstrapData(config, encryptionKey);

            System.out.println("Bootstrap initialization completed successfully");

        } catch (Exception e) {
            System.err.println("Error during bootstrap initialization: " + e.getMessage());
            throw e; // Re-throw to let the caller handle it
        }
    }

    private boolean acquireLock() throws InterruptedException {
        int remainingTimeout = Constants.BOOTSTRAP_TIMEOUT_SECONDS;
        while (remainingTimeout > 0) {
            Boolean acquired = jdbcTemplate.queryForObject("SELECT pg_try_advisory_lock(?)", Boolean.class, Constants.BOOTSTRAP_ADVISORY_LOCK_KEY);
            if (Boolean.TRUE.equals(acquired)) {
                System.out.println("Acquired bootstrap advisory lock");
                return true;
            }
            System.out.println(String.format("Waiting for bootstrap advisory lock... remaining timeout: %d seconds", remainingTimeout));
            Thread.sleep(5000);
            remainingTimeout -= 5;
        }
        return false;
    }

    private void releaseLock() {
        try {
            jdbcTemplate.queryForObject("SELECT pg_advisory_unlock(?)", Boolean.class, Constants.BOOTSTRAP_ADVISORY_LOCK_KEY);
            System.out.println("Released bootstrap advisory lock");
        } catch (Exception e) {
            System.err.println("Failed to release bootstrap advisory lock: " + e.getMessage());
        }
    }

    private String getBootstrapValue() {
        try {
            return jdbcTemplate.queryForObject("SELECT value FROM system_init WHERE primary_key = ?", String.class, BOOTSTRAP_DONE_KEY);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private static BootstrapConfig loadConfig(String configPath) throws IOException {
        return yamlMapper.readValue(new File(configPath), BootstrapConfig.class);
    }

    private static void generateApplicationYaml(BootstrapConfig config, String outputFolder, String encryptionKey) throws IOException {
        // Load the base application.yaml from classpath (included in the JAR)
        ApplicationConfig appConfig;
        try (var inputStream = BootstrapApplication.class.getClassLoader().getResourceAsStream("application.yaml")) {
            if (inputStream == null) {
                throw new IOException("application.yaml not found in classpath");
            }
            appConfig = yamlMapper.readValue(inputStream, ApplicationConfig.class);
        }

        // Encrypt the database password and update datasource configuration
        String encryptedPassword = EncryptionUtil.encrypt(config.getInfron().getDatasource().getPassword());
        if (appConfig.getSpring() != null && appConfig.getSpring().getDatasource() != null) {
            appConfig.getSpring().getDatasource().setUrl(config.getInfron().getDatasource().getUrl());
            appConfig.getSpring().getDatasource().setUsername(config.getInfron().getDatasource().getUsername());
            appConfig.getSpring().getDatasource().setPassword(encryptedPassword);
            appConfig.getSpring().getDatasource().setDriverClassName(config.getInfron().getDatasource().getDriverClassName());
        }

        // Create and add infron configuration
        ApplicationConfig.InfronConfig infronConfig = new ApplicationConfig.InfronConfig();
        infronConfig.setInstanceName(config.getInfron().getInstanceName());
        infronConfig.setInstanceId(config.getInfron().getInstanceId());

        appConfig.setInfron(infronConfig);

        // Write to output folder
        Files.createDirectories(Paths.get(outputFolder));
        try (var outputStream = Files.newOutputStream(Paths.get(outputFolder, "application.yaml"))) {
            yamlMapper.writeValue(outputStream, appConfig);
        }
        System.out.println("Generated application.yaml in " + outputFolder);
    }

    private static void runFlywayMigrations(BootstrapConfig config) {
        // Configure Flyway
        Flyway flyway = Flyway.configure()
            .dataSource(config.getInfron().getDatasource().getUrl(),
                       config.getInfron().getDatasource().getUsername(),
                       config.getInfron().getDatasource().getPassword())
            .locations("classpath:db/migration")
            .load();

        // Run migrations
        flyway.migrate();
        System.out.println("Flyway migrations completed");
    }

    private void insertBootstrapData(BootstrapConfig config, String encryptionKey) {
        // Insert IDP using repository
        IdentityProviderEntity idpEntity = insertIdentityProvider(config);

        // Insert system admin
        insertSystemAdmin(config, idpEntity.getId());

        // Insert tenants and tenant admins
        for (BootstrapConfig.TenantConfig tenant : config.getInfron().getTenants()) {
            insertTenantAndAdmin(tenant, config, idpEntity.getId());
        }

        // Mark bootstrap as done - for now using JDBC since system_init might not have a repository
        jdbcTemplate.update("INSERT INTO system_init (primary_key, value, updated_at) VALUES (?, ?, now()) ON CONFLICT (primary_key) DO UPDATE SET value = EXCLUDED.value, updated_at = now()",
                BOOTSTRAP_DONE_KEY, "true");

        System.out.println("Bootstrap data insertion completed");
    }

    private static String generateEncryptionKey(String instanceName, int instanceId, String passphrase) {
        try {
            // Create a deterministic key from instance details and passphrase
            String combined = instanceName + instanceId + passphrase;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Take first 16 bytes for AES-128 and base64 encode
            byte[] keyBytes = new byte[16];
            System.arraycopy(hash, 0, keyBytes, 0, 16);
            return Base64.getEncoder().encodeToString(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }

    private static void storeEncryptionKey(JdbcTemplate jdbcTemplate, String key) {
        jdbcTemplate.update("INSERT INTO system_init (primary_key, value, updated_at) VALUES (?, ?, now()) ON CONFLICT (primary_key) DO UPDATE SET value = EXCLUDED.value, updated_at = now()",
                           "encryption_key", key);
    }

    private IdentityProviderEntity insertIdentityProvider(BootstrapConfig config) {
        UUID idpId = UUID.fromString(Constants.DEFAULT_IDP_ID);
        Optional<IdentityProviderEntity> existing = identityProviderRepository.findById(idpId);
        IdentityProviderEntity idpEntity;

        if (existing.isPresent()) {
            idpEntity = existing.get();
            System.out.println("Identity provider already exists, updating: " + idpEntity.getName());
        } else {
            // Create IdentityProviderEntity using model classes
            String protocol = config.getInfron().getSystem().getIdp().getProtocol();
            if (IdentityProviderProtocol.OIDC.name().equals(protocol)) {
                idpEntity = new OidcIdentityProviderEntity();
            } else if (IdentityProviderProtocol.SAML2.name().equals(protocol)) {
                idpEntity = new Saml2IdentityProviderEntity();
            } else {
                // For other protocols, use base class and set protocol
                idpEntity = new IdentityProviderEntity();
                idpEntity.setProtocol(IdentityProviderProtocol.valueOf(protocol));
            }

            idpEntity.setId(idpId);
            idpEntity.setCreatedAt(java.time.Instant.now());
            System.out.println("Creating new identity provider: " + config.getInfron().getSystem().getIdp().getName());
        }

        // Update fields
        idpEntity.setName(config.getInfron().getSystem().getIdp().getName());
        idpEntity.setEnabled(true);
        idpEntity.setUpdatedAt(java.time.Instant.now());

        IdentityProviderProtocol protocolEnum = IdentityProviderProtocol.valueOf(config.getInfron().getSystem().getIdp().getProtocol().toUpperCase());
        idpEntity.setProtocol(protocolEnum);

        // Create proper metadata object based on protocol
        Object metadata;
        if (IdentityProviderProtocol.OIDC.equals(protocolEnum)) {
            OidcMetadata oidcMetadata = new OidcMetadata();
            Map<String, Object> rawMetadata = config.getInfron().getSystem().getIdp().getMetadata();
            oidcMetadata.setIssuerUri((String) rawMetadata.get("issuerUri"));
            oidcMetadata.setClientId((String) rawMetadata.get("clientId"));
            oidcMetadata.setClientSecret(EncryptionUtil.encrypt((String) rawMetadata.get("clientSecret")));
            metadata = oidcMetadata;
        } else if (IdentityProviderProtocol.SAML2.equals(protocolEnum)) {
            Saml2Metadata saml2Metadata = new Saml2Metadata();
            Map<String, Object> rawMetadata = config.getInfron().getSystem().getIdp().getMetadata();
            saml2Metadata.setEntityId((String) rawMetadata.get("entityId"));
            saml2Metadata.setSingleSignOnServiceUrl((String) rawMetadata.get("singleSignOnServiceUrl"));
            saml2Metadata.setPrivateKey(EncryptionUtil.encrypt((String) rawMetadata.get("privateKey")));
            metadata = saml2Metadata;
        } else {
            // Fallback to raw metadata map with encrypted secrets
            Map<String, Object> rawMetadata = config.getInfron().getSystem().getIdp().getMetadata();
            if (rawMetadata.containsKey("clientSecret")) {
                rawMetadata.put("clientSecret", EncryptionUtil.encrypt((String) rawMetadata.get("clientSecret")));
            }
            if (rawMetadata.containsKey("privateKey")) {
                rawMetadata.put("privateKey", EncryptionUtil.encrypt((String) rawMetadata.get("privateKey")));
            }
            metadata = rawMetadata;
        }

        // Serialize metadata and set it
        try {
            idpEntity.setMetadata(Map.of("data", yamlMapper.writeValueAsString(metadata)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize IDP metadata", e);
        }

        // Save using repository
        IdentityProviderEntity savedEntity = identityProviderRepository.save(idpEntity);

        System.out.println("Identity provider processed: " + savedEntity.getName());
        return savedEntity;
    }

    private void insertSystemAdmin(BootstrapConfig config, java.util.UUID idpId) {
        // Find system admin role using repository
        RoleEntity systemAdminRole = roleRepository.findByNameAndScopeId(Constants.ROLE_SYSTEM_ADMIN, UUID.fromString(Constants.SYSTEM_ID));
        if (systemAdminRole == null) {
            throw new RuntimeException("System admin role not found");
        }

        // Get OIDC connection details
        String issuerUri = (String) config.getInfron().getSystem().getIdp().getMetadata().get("issuerUri");
        String clientId = (String) config.getInfron().getSystem().getIdp().getMetadata().get("clientId");
        String clientSecret = (String) config.getInfron().getSystem().getIdp().getMetadata().get("clientSecret");

        // Add user and assign role
        IdpUserEntity savedUser = addUserToIdp(config.getInfron().getSystem().getSystemAdminUserName(),
                                               "System Admin", idpId, issuerUri, clientId, clientSecret);

        // Create and save role binding
        createRoleBinding(systemAdminRole, savedUser.getId(),
                         RoleScopeType.SYSTEM, java.util.UUID.fromString(Constants.SYSTEM_ID),
                         savedUser.getId());

        System.out.println("Inserted system admin user: " + savedUser.getUsername());
    }

    private void insertTenantAndAdmin(BootstrapConfig.TenantConfig tenantConfig, BootstrapConfig config, java.util.UUID idpId) {
        // Check if tenant already exists
        Optional<TenantEntity> existingTenant = tenantRepository.findAll().stream()
            .filter(t -> t.getName().equals(tenantConfig.getName()))
            .findFirst();

        TenantEntity tenantEntity;
        if (existingTenant.isPresent()) {
            tenantEntity = existingTenant.get();
            System.out.println("Tenant already exists, updating: " + tenantEntity.getName());
        } else {
            // Create TenantEntity using model classes
            tenantEntity = new TenantEntity();
            tenantEntity.setId(java.util.UUID.randomUUID());
            tenantEntity.setName(tenantConfig.getName());
            tenantEntity.setCreatedAt(java.time.Instant.now());
            System.out.println("Creating new tenant: " + tenantConfig.getName());
        }

        // Update fields
        tenantEntity.setStatus(TenantStatus.ACTIVE);
        tenantEntity.setUpdatedAt(java.time.Instant.now());

        // Save tenant using repository
        TenantEntity savedTenant = tenantRepository.save(tenantEntity);

        // Find tenant admin role using repository
        RoleEntity tenantAdminRole = roleRepository.findByNameAndScopeIdIsNull(Constants.ROLE_TENANT_ADMIN);
        if (tenantAdminRole == null) {
            throw new RuntimeException("Tenant admin role not found");
        }

        // Get OIDC connection details
        String issuerUri = (String) config.getInfron().getSystem().getIdp().getMetadata().get("issuerUri");
        String clientId = (String) config.getInfron().getSystem().getIdp().getMetadata().get("clientId");
        String clientSecret = (String) config.getInfron().getSystem().getIdp().getMetadata().get("clientSecret");

        // Add user and assign role
        IdpUserEntity savedUser = addUserToIdp(tenantConfig.getTenantAdminUserName(),
                                               "Tenant Admin", idpId, issuerUri, clientId, clientSecret);

        // Create and save role binding
        createRoleBinding(tenantAdminRole, savedUser.getId(),
                         RoleScopeType.TENANT, savedTenant.getId(),
                         savedUser.getId());

        System.out.println("Inserted tenant: " + savedTenant.getName() + " and admin user: " + savedUser.getUsername());
    }

    /**
     * Extracted method to add a user to the idp_users table.
     * This method can be reused for different types of admin users.
     */
    private IdpUserEntity addUserToIdp(String username, String defaultDisplayName, java.util.UUID idpId,
                                      String issuerUri, String clientId, String clientSecret) {

        // Check if user already exists
        Optional<IdpUserEntity> existingUser = idpUserRepository.findAll().stream()
            .filter(u -> u.getUsername().equals(username) && u.getIdentityProviderId().equals(idpId))
            .findFirst();

        IdpUserEntity userEntity;
        if (existingUser.isPresent()) {
            userEntity = existingUser.get();
            System.out.println("User already exists, updating: " + userEntity.getUsername());
        } else {
            // Get user info from OIDC
            OidcUserService oidcUserService = new OidcUserService(new RestTemplate(), new ObjectMapper());
            OidcUserInfo userInfo = oidcUserService.getUserByName(username, issuerUri, clientId, clientSecret);

            if (userInfo == null) {
                throw new RuntimeException("User not found in OIDC: " + username);
            }

            // Create IdpUserEntity using model classes
            userEntity = new IdpUserEntity();
            userEntity.setId(java.util.UUID.randomUUID());
            userEntity.setIdentityProviderId(idpId);
            userEntity.setExternalId(userInfo.getSub());
            userEntity.setCreatedAt(java.time.Instant.now());
            System.out.println("Creating new user: " + username);
        }

        // Update fields
        userEntity.setUsername(username);
        userEntity.setEmail(null); // Assuming email is not updated, or fetch again if needed
        userEntity.setDisplayName(defaultDisplayName);
        userEntity.setUpdatedAt(java.time.Instant.now());

        // Save user using repository
        return idpUserRepository.save(userEntity);
    }

    /**
     * Extracted method to create a role binding.
     * This method can be reused for different role assignments.
     */
    private RoleBindingEntity createRoleBinding(RoleEntity role, java.util.UUID subjectId,
                                               RoleScopeType scopeType, java.util.UUID scopeId,
                                               java.util.UUID createdBy) {
        // Check if role binding already exists
        Optional<RoleBindingEntity> existingBinding =
                roleBindingRepository.findBySubjectTypeAndSubjectId(RoleBindingSubjectType.USER, subjectId.toString())
                        .stream()
                        .filter(rb -> rb.getRole().getId().equals(role.getId())
                                && rb.getScopeType().equals(scopeType)
                                && (rb.getScopeId() == null ? scopeId == null : rb.getScopeId().equals(scopeId)))
                        .findFirst();

        if (existingBinding.isPresent()) {
            System.out.println("Role binding already exists for user " + subjectId + " and role " + role.getName());
            return existingBinding.get();
        }

        RoleBindingEntity roleBinding = new RoleBindingEntity();
        roleBinding.setId(java.util.UUID.randomUUID());
        roleBinding.setRole(role);  // Use setRole instead of setRoleId
        roleBinding.setSubjectType(RoleBindingSubjectType.USER);
        roleBinding.setSubjectId(subjectId.toString());
        roleBinding.setScopeType(scopeType);
        roleBinding.setScopeId(scopeId);
        roleBinding.setCreatedBy(createdBy);
        roleBinding.setCreatedAt(java.time.Instant.now());

        return roleBindingRepository.save(roleBinding);
    }
}
