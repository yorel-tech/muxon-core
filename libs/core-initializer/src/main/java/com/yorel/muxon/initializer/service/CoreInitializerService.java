/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.initializer.service;

import static com.yorel.muxon.common.Constants.BOOTSTRAP_STATUS_KEY;

import com.yorel.muxon.api.enums.BootstrapStatus;
import com.yorel.muxon.api.enums.RoleBindingSubjectType;
import com.yorel.muxon.api.enums.RoleScopeType;
import com.yorel.muxon.api.enums.TenantStatus;
import com.yorel.muxon.common.Constants;
import com.yorel.muxon.common.EncryptionUtil;
import com.yorel.muxon.db.model.IdentityProviderEntity;
import com.yorel.muxon.db.model.IdentityProviderProtocol;
import com.yorel.muxon.db.model.IdpUserEntity;
import com.yorel.muxon.db.model.OidcIdentityProviderEntity;
import com.yorel.muxon.db.model.OidcMetadata;
import com.yorel.muxon.db.model.RoleBindingEntity;
import com.yorel.muxon.db.model.Saml2IdentityProviderEntity;
import com.yorel.muxon.db.model.Saml2Metadata;
import com.yorel.muxon.db.model.TenantEntity;
import com.yorel.muxon.db.repository.IdentityProviderRepository;
import com.yorel.muxon.db.repository.IdpUserRepository;
import com.yorel.muxon.db.repository.RoleBindingRepository;
import com.yorel.muxon.db.repository.TenantRepository;
import com.yorel.muxon.initializer.config.ApplicationConfig;
import com.yorel.muxon.initializer.config.BootstrapConfig;
import com.yorel.muxon.services.OidcUserService;
import com.yorel.muxon.services.model.OidcUserInfo;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

@Service
public class CoreInitializerService {

  /**
   * Same default as {@code spring.flyway.placeholders.contentStorageDefaultPath} in core-services.
   */
  private static final String DEFAULT_CONTENT_STORAGE_ROOT = "/var/lib/muxon/content-libraries";

  private static final ObjectMapper yamlMapper =
      YAMLMapper.builder()
          .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .build();

  public record ServiceTemplate(String serviceName, String templateFile) {}

  private static final List<ServiceTemplate> OSS_SERVICES =
      List.of(
          new ServiceTemplate("core-services", "core-services-template.yaml"),
          new ServiceTemplate("orchestrator", "orchestrator-template.yaml"),
          new ServiceTemplate("console-proxy", "console-proxy-template.yaml"));

  @Autowired private IdentityProviderRepository identityProviderRepository;

  @Autowired private IdpUserRepository idpUserRepository;

  @Autowired private TenantRepository tenantRepository;

  @Autowired private RoleBindingRepository roleBindingRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  /** Full OSS initialisation: Flyway + DB seed + all three per-service config files. */
  public void performOssInitialization(
      String configPath,
      String outputFolder,
      String passphrasePath,
      String oidcSecretPath,
      String dbPasswordPath)
      throws Exception {
    BootstrapConfig config = loadConfig(configPath);
    String dbPassword = Files.readString(Paths.get(dbPasswordPath)).trim();

    // Flyway must run before reading system_init (that table is created by migrations).
    runFlywayMigrations(config, dbPassword);

    BootstrapStatus bootstrapValue = getBootstrapValue();
    if (!BootstrapStatus.NOTREADY.equals(bootstrapValue)) {
      System.out.println("Bootstrap already completed");
      return;
    }

    String passphrase = Files.readString(Paths.get(passphrasePath)).trim();

    String encryptionKey =
        generateEncryptionKey(
            config.getMuxon().getInstanceName(), config.getMuxon().getInstanceId(), passphrase);
    EncryptionUtil.setKey(encryptionKey);

    generateAllApplicationYamls(config, outputFolder, encryptionKey, dbPassword);
    insertBootstrapData(config, encryptionKey, oidcSecretPath);
  }

  public boolean acquireLock() throws InterruptedException {
    int remainingTimeout = Constants.BOOTSTRAP_TIMEOUT_SECONDS;
    while (remainingTimeout > 0) {
      Boolean acquired =
          jdbcTemplate.queryForObject(
              "SELECT pg_try_advisory_lock(?)",
              Boolean.class,
              Constants.BOOTSTRAP_ADVISORY_LOCK_KEY);
      if (Boolean.TRUE.equals(acquired)) {
        System.out.println("Acquired bootstrap advisory lock");
        return true;
      }
      System.out.printf(
          "Waiting for bootstrap advisory lock... remaining timeout: %d seconds%n",
          remainingTimeout);
      Thread.sleep(5000);
      remainingTimeout -= 5;
    }
    return false;
  }

  public void releaseLock() {
    try {
      jdbcTemplate.queryForObject(
          "SELECT pg_advisory_unlock(?)", Boolean.class, Constants.BOOTSTRAP_ADVISORY_LOCK_KEY);
      System.out.println("Released bootstrap advisory lock");
    } catch (Exception e) {
      System.err.println("Failed to release bootstrap advisory lock: " + e.getMessage());
    }
  }

  public BootstrapStatus getBootstrapValue() {
    try {
      final String bootStrapStatus =
          jdbcTemplate.queryForObject(
              "SELECT system_status::text FROM system_init WHERE primary_key = ?",
              String.class,
              BOOTSTRAP_STATUS_KEY);
      return BootstrapStatus.valueOf(bootStrapStatus);
    } catch (EmptyResultDataAccessException e) {
      return BootstrapStatus.NOTREADY;
    }
  }

  public void runFlywayMigrations(BootstrapConfig config, String dbPassword) {
    Flyway flyway =
        Flyway.configure()
            .dataSource(
                config.getMuxon().getDatasource().getUrl(),
                config.getMuxon().getDatasource().getUsername(),
                dbPassword)
            .locations("classpath:db/migration/oss")
            .placeholders(Map.of("contentStorageDefaultPath", DEFAULT_CONTENT_STORAGE_ROOT))
            .load();

    var result = flyway.migrate();
    System.out.printf(
        "Flyway migrations completed (%d migration(s) applied, success: %b)%n",
        result.migrationsExecuted, result.success);
  }

  public static BootstrapConfig loadConfig(String configPath) throws IOException {
    return yamlMapper.readValue(new File(configPath), BootstrapConfig.class);
  }

  public void generateAllApplicationYamls(
      BootstrapConfig config, String outputFolder, String encryptionKey, String dbPassword)
      throws IOException {
    for (ServiceTemplate service : OSS_SERVICES) {
      generateApplicationYamlForService(
          service.serviceName(),
          service.templateFile(),
          config,
          outputFolder,
          encryptionKey,
          dbPassword);
    }
  }

  public void generateApplicationYamlForService(
      String serviceName,
      String templateFile,
      BootstrapConfig config,
      String outputFolder,
      String encryptionKey,
      String dbPassword)
      throws IOException {
    ApplicationConfig appConfig = loadApplicationTemplateFromClasspath(templateFile);

    String encryptedPassword = EncryptionUtil.encrypt(dbPassword);
    if (appConfig.getSpring() != null && appConfig.getSpring().getDatasource() != null) {
      appConfig.getSpring().getDatasource().setUrl(config.getMuxon().getDatasource().getUrl());
      appConfig
          .getSpring()
          .getDatasource()
          .setUsername(config.getMuxon().getDatasource().getUsername());
      appConfig.getSpring().getDatasource().setPassword(encryptedPassword);
      appConfig
          .getSpring()
          .getDatasource()
          .setDriverClassName(config.getMuxon().getDatasource().getDriverClassName());
    }

    // Ensure Flyway placeholder used by migrations is present in emitted application.yaml.
    if (appConfig.getSpring() != null) {
      if (appConfig.getSpring().getFlyway() == null) {
        appConfig.getSpring().setFlyway(new ApplicationConfig.SpringConfig.FlywayConfig());
      }
      var flyway = appConfig.getSpring().getFlyway();
      if (flyway.getPlaceholders() == null || flyway.getPlaceholders().isEmpty()) {
        flyway.setPlaceholders(Map.of("contentStorageDefaultPath", DEFAULT_CONTENT_STORAGE_ROOT));
      } else if (!flyway.getPlaceholders().containsKey("contentStorageDefaultPath")) {
        var merged = new java.util.HashMap<>(flyway.getPlaceholders());
        merged.put("contentStorageDefaultPath", DEFAULT_CONTENT_STORAGE_ROOT);
        flyway.setPlaceholders(merged);
      }
    }

    if (appConfig.getMuxon() == null) {
      appConfig.setMuxon(new ApplicationConfig.MuxonConfig());
    }
    appConfig.getMuxon().setInstanceName(config.getMuxon().getInstanceName());
    appConfig.getMuxon().setInstanceId(config.getMuxon().getInstanceId());

    Files.createDirectories(Paths.get(outputFolder));
    var outPath = Paths.get(outputFolder, serviceName + "-application.yaml");
    try (var outputStream = Files.newOutputStream(outPath)) {
      yamlMapper.writeValue(outputStream, appConfig);
    }
    System.out.println("Generated " + outPath);
  }

  private static ApplicationConfig loadApplicationTemplateFromClasspath(String templateFile)
      throws IOException {
    try (InputStream inputStream =
        CoreInitializerService.class.getClassLoader().getResourceAsStream(templateFile)) {
      if (inputStream == null) {
        throw new IOException(templateFile + " not found in classpath");
      }
      return yamlMapper.readValue(inputStream, ApplicationConfig.class);
    }
  }

  private void insertBootstrapData(
      BootstrapConfig config, String encryptionKey, String oidcSecretPath) throws IOException {
    IdentityProviderEntity idpEntity = insertIdentityProvider(config, oidcSecretPath);
    insertSystemAdmin(config, idpEntity.getId(), oidcSecretPath);

    if (config.getMuxon().getTenants() != null) {
      for (BootstrapConfig.TenantConfig tenant : config.getMuxon().getTenants()) {
        insertTenantAndAdmin(tenant, config, idpEntity.getId(), oidcSecretPath);
      }
    }

    jdbcTemplate.update(
        "INSERT INTO system_init (primary_key, value, system_status, updated_at) VALUES (?, ?, CAST(? AS bootstrap_status), now()) "
            + "ON CONFLICT (primary_key) DO UPDATE SET value = EXCLUDED.value, system_status = EXCLUDED.system_status, updated_at = now()",
        BOOTSTRAP_STATUS_KEY,
        BootstrapStatus.BOOTSTRAPPED.name(),
        BootstrapStatus.BOOTSTRAPPED.name());
  }

  private IdentityProviderEntity insertIdentityProvider(
      BootstrapConfig config, String oidcSecretPath) throws IOException {
    UUID idpId = UUID.fromString(Constants.DEFAULT_IDP_ID);
    Optional<IdentityProviderEntity> existing = identityProviderRepository.findById(idpId);
    IdentityProviderEntity idpEntity;

    if (existing.isPresent()) {
      idpEntity = existing.get();
      System.out.println("Identity provider already exists, updating: " + idpEntity.getName());
    } else {
      String protocol = config.getMuxon().getSystem().getIdp().getProtocol();
      if (IdentityProviderProtocol.OIDC.name().equals(protocol)) {
        idpEntity = new OidcIdentityProviderEntity();
      } else if (IdentityProviderProtocol.SAML2.name().equals(protocol)) {
        idpEntity = new Saml2IdentityProviderEntity();
      } else {
        idpEntity = new IdentityProviderEntity();
        idpEntity.setProtocol(IdentityProviderProtocol.valueOf(protocol));
      }

      idpEntity.setId(idpId);
      idpEntity.setCreatedAt(java.time.Instant.now());
      System.out.println(
          "Creating new identity provider: " + config.getMuxon().getSystem().getIdp().getName());
    }

    idpEntity.setName(config.getMuxon().getSystem().getIdp().getName());
    idpEntity.setEnabled(true);
    idpEntity.setUpdatedAt(java.time.Instant.now());

    IdentityProviderProtocol protocolEnum =
        IdentityProviderProtocol.valueOf(
            config.getMuxon().getSystem().getIdp().getProtocol().toUpperCase());
    idpEntity.setProtocol(protocolEnum);

    if (IdentityProviderProtocol.OIDC.equals(protocolEnum)) {
      OidcMetadata oidcMetadata = getOidcMetadata(config, oidcSecretPath);
      ((OidcIdentityProviderEntity) idpEntity).setOidcMetadata(oidcMetadata);
    } else if (IdentityProviderProtocol.SAML2.equals(protocolEnum)) {
      Saml2Metadata saml2Metadata = getSaml2Metadata(config);
      ((Saml2IdentityProviderEntity) idpEntity).setSaml2Metadata(saml2Metadata);
    } else {
      Map<String, String> rawMetadata = config.getMuxon().getSystem().getIdp().getMetadata();
      if (rawMetadata.containsKey("clientSecret")) {
        rawMetadata.put("clientSecret", EncryptionUtil.encrypt(rawMetadata.get("clientSecret")));
      }
      if (rawMetadata.containsKey("privateKey")) {
        rawMetadata.put("privateKey", EncryptionUtil.encrypt(rawMetadata.get("privateKey")));
      }
      idpEntity.setMetadata(rawMetadata);
    }

    return identityProviderRepository.save(idpEntity);
  }

  private static Saml2Metadata getSaml2Metadata(BootstrapConfig config) {
    Saml2Metadata saml2Metadata = new Saml2Metadata();
    Map<String, String> rawMetadata = config.getMuxon().getSystem().getIdp().getMetadata();
    saml2Metadata.setEntityId(rawMetadata.get("entityId"));
    saml2Metadata.setSingleSignOnServiceUrl(rawMetadata.get("singleSignOnServiceUrl"));
    saml2Metadata.setPrivateKey(EncryptionUtil.encrypt(rawMetadata.get("privateKey")));
    return saml2Metadata;
  }

  private static OidcMetadata getOidcMetadata(BootstrapConfig config, String oidcSecretPath)
      throws IOException {
    OidcMetadata oidcMetadata = new OidcMetadata();
    Map<String, String> rawMetadata = config.getMuxon().getSystem().getIdp().getMetadata();
    oidcMetadata.setIssuerUri(rawMetadata.get("issuerUri"));
    oidcMetadata.setClientId(rawMetadata.get("clientId"));
    String clientSecret = Files.readString(Paths.get(oidcSecretPath)).trim();
    oidcMetadata.setClientSecret(EncryptionUtil.encrypt(clientSecret));
    return oidcMetadata;
  }

  private void insertSystemAdmin(BootstrapConfig config, UUID idpId, String oidcSecretPath)
      throws IOException {
    String issuerUri = config.getMuxon().getSystem().getIdp().getMetadata().get("issuerUri");
    String clientId = config.getMuxon().getSystem().getIdp().getMetadata().get("clientId");
    String clientSecret = Files.readString(Paths.get(oidcSecretPath)).trim();

    IdpUserEntity savedUser =
        addUserToIdp(
            config.getMuxon().getSystem().getSystemAdminUserName(),
            "System Admin",
            idpId,
            issuerUri,
            clientId,
            clientSecret);

    createRoleBinding(
        Constants.ROLE_SYSTEM_ADMIN,
        savedUser.getId(),
        RoleScopeType.SYSTEM,
        UUID.fromString(Constants.SYSTEM_ID),
        savedUser.getId());
  }

  private void insertTenantAndAdmin(
      BootstrapConfig.TenantConfig tenantConfig,
      BootstrapConfig config,
      UUID idpId,
      String oidcSecretPath)
      throws IOException {
    Optional<TenantEntity> existingTenant =
        tenantRepository.findAll().stream()
            .filter(t -> t.getName().equals(tenantConfig.getName()))
            .findFirst();

    TenantEntity tenantEntity;
    if (existingTenant.isPresent()) {
      tenantEntity = existingTenant.get();
      System.out.println("Tenant already exists, updating: " + tenantEntity.getName());
    } else {
      tenantEntity = new TenantEntity();
      tenantEntity.setId(UUID.randomUUID());
      tenantEntity.setName(tenantConfig.getName());
      tenantEntity.setCreatedAt(java.time.Instant.now());
      System.out.println("Creating new tenant: " + tenantConfig.getName());
    }

    tenantEntity.setStatus(TenantStatus.ACTIVE);
    tenantEntity.setUpdatedAt(java.time.Instant.now());
    TenantEntity savedTenant = tenantRepository.save(tenantEntity);

    String issuerUri = config.getMuxon().getSystem().getIdp().getMetadata().get("issuerUri");
    String clientId = config.getMuxon().getSystem().getIdp().getMetadata().get("clientId");
    String clientSecret = Files.readString(Paths.get(oidcSecretPath)).trim();

    IdpUserEntity savedUser =
        addUserToIdp(
            tenantConfig.getTenantAdminUserName(),
            "Tenant Admin",
            idpId,
            issuerUri,
            clientId,
            clientSecret);

    createRoleBinding(
        Constants.ROLE_TENANT_ADMIN,
        savedUser.getId(),
        RoleScopeType.TENANT,
        savedTenant.getId(),
        savedUser.getId());
  }

  private IdpUserEntity addUserToIdp(
      String username,
      String defaultDisplayName,
      UUID idpId,
      String issuerUri,
      String clientId,
      String clientSecret) {
    Optional<IdpUserEntity> existingUser =
        idpUserRepository.findAll().stream()
            .filter(
                u -> u.getUsername().equals(username) && u.getIdentityProviderId().equals(idpId))
            .findFirst();

    IdpUserEntity userEntity;
    if (existingUser.isPresent()) {
      userEntity = existingUser.get();
      System.out.println("User already exists, updating: " + userEntity.getUsername());
    } else {
      OidcUserService oidcUserService = new OidcUserService(new RestTemplate(), new ObjectMapper());
      OidcUserInfo userInfo =
          oidcUserService.getUserByName(username, issuerUri, clientId, clientSecret);

      if (userInfo == null) {
        throw new RuntimeException("User not found in OIDC: " + username);
      }

      userEntity = new IdpUserEntity();
      userEntity.setId(UUID.randomUUID());
      userEntity.setIdentityProviderId(idpId);
      userEntity.setExternalId(userInfo.getSub());
      userEntity.setCreatedAt(java.time.Instant.now());
      System.out.println("Creating new user: " + username);
    }

    userEntity.setUsername(username);
    userEntity.setEmail(null);
    userEntity.setDisplayName(defaultDisplayName);
    userEntity.setUpdatedAt(java.time.Instant.now());

    return idpUserRepository.save(userEntity);
  }

  private RoleBindingEntity createRoleBinding(
      String roleName, UUID subjectId, RoleScopeType scopeType, UUID scopeId, UUID createdBy) {
    Optional<RoleBindingEntity> existingBinding =
        roleBindingRepository
            .findBySubjectTypeAndSubjectId(RoleBindingSubjectType.USER, subjectId.toString())
            .stream()
            .filter(
                rb ->
                    roleName.equals(rb.getRoleName())
                        && rb.getScopeType().equals(scopeType)
                        && (rb.getScopeId() == null
                            ? scopeId == null
                            : rb.getScopeId().equals(scopeId)))
            .findFirst();

    if (existingBinding.isPresent()) {
      System.out.println(
          "Role binding already exists for user " + subjectId + " and role " + roleName);
      return existingBinding.get();
    }

    RoleBindingEntity roleBinding = new RoleBindingEntity();
    roleBinding.setId(UUID.randomUUID());
    roleBinding.setRoleName(roleName);
    roleBinding.setSubjectType(RoleBindingSubjectType.USER);
    roleBinding.setSubjectId(subjectId.toString());
    roleBinding.setScopeType(scopeType);
    roleBinding.setScopeId(scopeId);
    roleBinding.setCreatedBy(createdBy);
    roleBinding.setCreatedAt(java.time.Instant.now());

    return roleBindingRepository.save(roleBinding);
  }

  private static String generateEncryptionKey(
      String instanceName, int instanceId, String passphrase) {
    try {
      String combined = instanceName + instanceId + passphrase;
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(combined.getBytes(java.nio.charset.StandardCharsets.UTF_8));

      byte[] keyBytes = new byte[16];
      System.arraycopy(hash, 0, keyBytes, 0, 16);
      return Base64.getEncoder().encodeToString(keyBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Failed to generate encryption key", e);
    }
  }
}
