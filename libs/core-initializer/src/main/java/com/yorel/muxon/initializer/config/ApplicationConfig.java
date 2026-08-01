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
package com.yorel.muxon.initializer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/** Configuration class representing the service template YAML structure. */
public class ApplicationConfig {

  private ServerConfig server;
  private ManagementConfig management;
  private SpringConfig spring;
  private SpringDocConfig springdoc;
  private LoggingConfig logging;
  private MuxonConfig muxon;

  public ServerConfig getServer() {
    return server;
  }

  public void setServer(ServerConfig server) {
    this.server = server;
  }

  public ManagementConfig getManagement() {
    return management;
  }

  public void setManagement(ManagementConfig management) {
    this.management = management;
  }

  public SpringConfig getSpring() {
    return spring;
  }

  public void setSpring(SpringConfig spring) {
    this.spring = spring;
  }

  public SpringDocConfig getSpringdoc() {
    return springdoc;
  }

  public void setSpringdoc(SpringDocConfig springdoc) {
    this.springdoc = springdoc;
  }

  public LoggingConfig getLogging() {
    return logging;
  }

  public void setLogging(LoggingConfig logging) {
    this.logging = logging;
  }

  public MuxonConfig getMuxon() {
    return muxon;
  }

  public void setMuxon(MuxonConfig muxon) {
    this.muxon = muxon;
  }

  public static class ServerConfig {
    private Integer port;

    public Integer getPort() {
      return port;
    }

    public void setPort(Integer port) {
      this.port = port;
    }
  }

  public static class ManagementConfig {
    private EndpointsConfig endpoints;
    private MetricsConfig metrics;
    private TracingConfig tracing;

    public EndpointsConfig getEndpoints() {
      return endpoints;
    }

    public void setEndpoints(EndpointsConfig endpoints) {
      this.endpoints = endpoints;
    }

    public MetricsConfig getMetrics() {
      return metrics;
    }

    public void setMetrics(MetricsConfig metrics) {
      this.metrics = metrics;
    }

    public TracingConfig getTracing() {
      return tracing;
    }

    public void setTracing(TracingConfig tracing) {
      this.tracing = tracing;
    }

    public static class EndpointsConfig {
      private WebConfig web;

      public WebConfig getWeb() {
        return web;
      }

      public void setWeb(WebConfig web) {
        this.web = web;
      }

      public static class WebConfig {
        private ExposureConfig exposure;

        public ExposureConfig getExposure() {
          return exposure;
        }

        public void setExposure(ExposureConfig exposure) {
          this.exposure = exposure;
        }

        public static class ExposureConfig {
          private String include;

          public String getInclude() {
            return include;
          }

          public void setInclude(String include) {
            this.include = include;
          }
        }
      }
    }

    public static class MetricsConfig {
      private ExportConfig export;

      public ExportConfig getExport() {
        return export;
      }

      public void setExport(ExportConfig export) {
        this.export = export;
      }

      public static class ExportConfig {
        private PrometheusConfig prometheus;

        public PrometheusConfig getPrometheus() {
          return prometheus;
        }

        public void setPrometheus(PrometheusConfig prometheus) {
          this.prometheus = prometheus;
        }

        public static class PrometheusConfig {
          private Boolean enabled;

          public Boolean getEnabled() {
            return enabled;
          }

          public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
          }
        }
      }
    }

    public static class TracingConfig {
      private SamplingConfig sampling;

      public SamplingConfig getSampling() {
        return sampling;
      }

      public void setSampling(SamplingConfig sampling) {
        this.sampling = sampling;
      }

      public static class SamplingConfig {
        private Double probability;

        public Double getProbability() {
          return probability;
        }

        public void setProbability(Double probability) {
          this.probability = probability;
        }
      }
    }
  }

  public static class SpringConfig {
    private MainConfig main;
    private SpringApplicationConfig application;
    private GrpcConfig grpc;
    private TaskConfig task;
    private DatasourceConfig datasource;
    private JpaConfig jpa;
    private SecurityConfig security;
    private FlywayConfig flyway;

    public MainConfig getMain() {
      return main;
    }

    public void setMain(MainConfig main) {
      this.main = main;
    }

    public SpringApplicationConfig getApplication() {
      return application;
    }

    public void setApplication(SpringApplicationConfig application) {
      this.application = application;
    }

    public GrpcConfig getGrpc() {
      return grpc;
    }

    public void setGrpc(GrpcConfig grpc) {
      this.grpc = grpc;
    }

    public TaskConfig getTask() {
      return task;
    }

    public void setTask(TaskConfig task) {
      this.task = task;
    }

    public DatasourceConfig getDatasource() {
      return datasource;
    }

    public void setDatasource(DatasourceConfig datasource) {
      this.datasource = datasource;
    }

    public JpaConfig getJpa() {
      return jpa;
    }

    public void setJpa(JpaConfig jpa) {
      this.jpa = jpa;
    }

    public SecurityConfig getSecurity() {
      return security;
    }

    public void setSecurity(SecurityConfig security) {
      this.security = security;
    }

    public FlywayConfig getFlyway() {
      return flyway;
    }

    public void setFlyway(FlywayConfig flyway) {
      this.flyway = flyway;
    }

    public static class MainConfig {
      @JsonProperty("web-application-type")
      private String webApplicationType;

      public String getWebApplicationType() {
        return webApplicationType;
      }

      public void setWebApplicationType(String webApplicationType) {
        this.webApplicationType = webApplicationType;
      }
    }

    public static class SpringApplicationConfig {
      private String name;

      public String getName() {
        return name;
      }

      public void setName(String name) {
        this.name = name;
      }
    }

    public static class GrpcConfig {
      private Server server;

      public Server getServer() {
        return server;
      }

      public void setServer(Server server) {
        this.server = server;
      }

      public static class Server {
        private Integer port;

        public Integer getPort() {
          return port;
        }

        public void setPort(Integer port) {
          this.port = port;
        }
      }
    }

    public static class TaskConfig {
      private Scheduling scheduling;

      public Scheduling getScheduling() {
        return scheduling;
      }

      public void setScheduling(Scheduling scheduling) {
        this.scheduling = scheduling;
      }

      public static class Scheduling {
        private Pool pool;

        public Pool getPool() {
          return pool;
        }

        public void setPool(Pool pool) {
          this.pool = pool;
        }

        public static class Pool {
          private Integer size;

          public Integer getSize() {
            return size;
          }

          public void setSize(Integer size) {
            this.size = size;
          }
        }
      }
    }

    public static class DatasourceConfig {
      private String url;
      private String username;
      private String password;

      @JsonProperty("driver-class-name")
      private String driverClassName;

      public String getUrl() {
        return url;
      }

      public void setUrl(String url) {
        this.url = url;
      }

      public String getUsername() {
        return username;
      }

      public void setUsername(String username) {
        this.username = username;
      }

      public String getPassword() {
        return password;
      }

      public void setPassword(String password) {
        this.password = password;
      }

      public String getDriverClassName() {
        return driverClassName;
      }

      public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
      }
    }

    public static class JpaConfig {
      private HibernateConfig hibernate;
      private PropertiesConfig properties;

      public HibernateConfig getHibernate() {
        return hibernate;
      }

      public void setHibernate(HibernateConfig hibernate) {
        this.hibernate = hibernate;
      }

      public PropertiesConfig getProperties() {
        return properties;
      }

      public void setProperties(PropertiesConfig properties) {
        this.properties = properties;
      }

      public static class HibernateConfig {
        @JsonProperty("ddl-auto")
        private String ddlAuto;

        public String getDdlAuto() {
          return ddlAuto;
        }

        public void setDdlAuto(String ddlAuto) {
          this.ddlAuto = ddlAuto;
        }
      }

      public static class PropertiesConfig {
        private HibernateProperties hibernate;

        public HibernateProperties getHibernate() {
          return hibernate;
        }

        public void setHibernate(HibernateProperties hibernate) {
          this.hibernate = hibernate;
        }

        public static class HibernateProperties {
          private String dialect;

          public String getDialect() {
            return dialect;
          }

          public void setDialect(String dialect) {
            this.dialect = dialect;
          }
        }
      }
    }

    public static class SecurityConfig {
      private Oauth2Config oauth2;

      public Oauth2Config getOauth2() {
        return oauth2;
      }

      public void setOauth2(Oauth2Config oauth2) {
        this.oauth2 = oauth2;
      }

      public static class Oauth2Config {
        private ResourceserverConfig resourceserver;
        private ClientConfig client;

        public ResourceserverConfig getResourceserver() {
          return resourceserver;
        }

        public void setResourceserver(ResourceserverConfig resourceserver) {
          this.resourceserver = resourceserver;
        }

        public ClientConfig getClient() {
          return client;
        }

        public void setClient(ClientConfig client) {
          this.client = client;
        }

        public static class ResourceserverConfig {
          private JwtConfig jwt;

          public JwtConfig getJwt() {
            return jwt;
          }

          public void setJwt(JwtConfig jwt) {
            this.jwt = jwt;
          }

          public static class JwtConfig {
            @JsonProperty("issuer-uri")
            private String issuerUri;

            public String getIssuerUri() {
              return issuerUri;
            }

            public void setIssuerUri(String issuerUri) {
              this.issuerUri = issuerUri;
            }
          }
        }

        public static class ClientConfig {
          private Map<String, RegistrationConfig> registration;

          public Map<String, RegistrationConfig> getRegistration() {
            return registration;
          }

          public void setRegistration(Map<String, RegistrationConfig> registration) {
            this.registration = registration;
          }

          public static class RegistrationConfig {
            @JsonProperty("client-secret")
            private String clientSecret;

            public String getClientSecret() {
              return clientSecret;
            }

            public void setClientSecret(String clientSecret) {
              this.clientSecret = clientSecret;
            }
          }
        }
      }
    }

    public static class FlywayConfig {
      private Boolean enabled;
      private String locations;
      private Map<String, String> placeholders;

      public Boolean getEnabled() {
        return enabled;
      }

      public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
      }

      public String getLocations() {
        return locations;
      }

      public void setLocations(String locations) {
        this.locations = locations;
      }

      public Map<String, String> getPlaceholders() {
        return placeholders;
      }

      public void setPlaceholders(Map<String, String> placeholders) {
        this.placeholders = placeholders;
      }
    }
  }

  public static class SpringDocConfig {
    @JsonProperty("writer-with-order-by-keys")
    private Boolean writerWithOrderByKeys;

    @JsonProperty("swagger-ui")
    private SwaggerUiConfig swaggerUi;

    public Boolean getWriterWithOrderByKeys() {
      return writerWithOrderByKeys;
    }

    public void setWriterWithOrderByKeys(Boolean writerWithOrderByKeys) {
      this.writerWithOrderByKeys = writerWithOrderByKeys;
    }

    public SwaggerUiConfig getSwaggerUi() {
      return swaggerUi;
    }

    public void setSwaggerUi(SwaggerUiConfig swaggerUi) {
      this.swaggerUi = swaggerUi;
    }

    public static class SwaggerUiConfig {
      @JsonProperty("try-it-out-enabled")
      private Boolean tryItOutEnabled;

      public Boolean getTryItOutEnabled() {
        return tryItOutEnabled;
      }

      public void setTryItOutEnabled(Boolean tryItOutEnabled) {
        this.tryItOutEnabled = tryItOutEnabled;
      }
    }
  }

  public static class LoggingConfig {
    private LevelConfig level;
    private FileConfig file;
    private PatternConfig pattern;

    public LevelConfig getLevel() {
      return level;
    }

    public void setLevel(LevelConfig level) {
      this.level = level;
    }

    public FileConfig getFile() {
      return file;
    }

    public void setFile(FileConfig file) {
      this.file = file;
    }

    public PatternConfig getPattern() {
      return pattern;
    }

    public void setPattern(PatternConfig pattern) {
      this.pattern = pattern;
    }

    public static class LevelConfig {
      private String root;

      @JsonProperty("org.springframework")
      private String orgSpringframework;

      @JsonProperty("org.hibernate")
      private String orgHibernate;

      @JsonProperty("com.yorel.muxon")
      private String comKritoMuxon;

      public String getRoot() {
        return root;
      }

      public void setRoot(String root) {
        this.root = root;
      }

      public String getOrgSpringframework() {
        return orgSpringframework;
      }

      public void setOrgSpringframework(String orgSpringframework) {
        this.orgSpringframework = orgSpringframework;
      }

      public String getOrgHibernate() {
        return orgHibernate;
      }

      public void setOrgHibernate(String orgHibernate) {
        this.orgHibernate = orgHibernate;
      }

      public String getComKritoMuxon() {
        return comKritoMuxon;
      }

      public void setComKritoMuxon(String comKritoMuxon) {
        this.comKritoMuxon = comKritoMuxon;
      }
    }

    public static class FileConfig {
      private String name;

      public String getName() {
        return name;
      }

      public void setName(String name) {
        this.name = name;
      }
    }

    public static class PatternConfig {
      private String console;
      private String file;

      public String getConsole() {
        return console;
      }

      public void setConsole(String console) {
        this.console = console;
      }

      public String getFile() {
        return file;
      }

      public void setFile(String file) {
        this.file = file;
      }
    }
  }

  public static class MuxonConfig {
    private String instanceName;
    private int instanceId;
    private EnterpriseConfig enterprise;
    private SystemConfig system;
    private List<TenantConfig> tenants;
    private QueueConfig queue;
    private WorkerConfig worker;
    private ProviderConfig provider;
    private ConsoleConfig console;

    public String getInstanceName() {
      return instanceName;
    }

    public void setInstanceName(String instanceName) {
      this.instanceName = instanceName;
    }

    public int getInstanceId() {
      return instanceId;
    }

    public void setInstanceId(int instanceId) {
      this.instanceId = instanceId;
    }

    public EnterpriseConfig getEnterprise() {
      return enterprise;
    }

    public void setEnterprise(EnterpriseConfig enterprise) {
      this.enterprise = enterprise;
    }

    public SystemConfig getSystem() {
      return system;
    }

    public void setSystem(SystemConfig system) {
      this.system = system;
    }

    public List<TenantConfig> getTenants() {
      return tenants;
    }

    public void setTenants(List<TenantConfig> tenants) {
      this.tenants = tenants;
    }

    public QueueConfig getQueue() {
      return queue;
    }

    public void setQueue(QueueConfig queue) {
      this.queue = queue;
    }

    public WorkerConfig getWorker() {
      return worker;
    }

    public void setWorker(WorkerConfig worker) {
      this.worker = worker;
    }

    public ProviderConfig getProvider() {
      return provider;
    }

    public void setProvider(ProviderConfig provider) {
      this.provider = provider;
    }

    public ConsoleConfig getConsole() {
      return console;
    }

    public void setConsole(ConsoleConfig console) {
      this.console = console;
    }

    public static class EnterpriseConfig {
      private Boolean enabled;

      public Boolean getEnabled() {
        return enabled;
      }

      public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
      }
    }

    public static class SystemConfig {
      @JsonProperty("systemAdminUserName")
      private String systemAdminUserName;

      public String getSystemAdminUserName() {
        return systemAdminUserName;
      }

      public void setSystemAdminUserName(String systemAdminUserName) {
        this.systemAdminUserName = systemAdminUserName;
      }
    }

    public static class TenantConfig {
      private String name;

      @JsonProperty("tenantAdminUserName")
      private String tenantAdminUserName;

      public String getName() {
        return name;
      }

      public void setName(String name) {
        this.name = name;
      }

      public String getTenantAdminUserName() {
        return tenantAdminUserName;
      }

      public void setTenantAdminUserName(String tenantAdminUserName) {
        this.tenantAdminUserName = tenantAdminUserName;
      }
    }

    public static class QueueConfig {
      private String backend;

      public String getBackend() {
        return backend;
      }

      public void setBackend(String backend) {
        this.backend = backend;
      }
    }

    public static class WorkerConfig {
      @JsonProperty("poll-delay-ms")
      private Integer pollDelayMs;

      @JsonProperty("poll-batch-size")
      private Integer pollBatchSize;

      @JsonProperty("stall-threshold-minutes")
      private Integer stallThresholdMinutes;

      public Integer getPollDelayMs() {
        return pollDelayMs;
      }

      public void setPollDelayMs(Integer pollDelayMs) {
        this.pollDelayMs = pollDelayMs;
      }

      public Integer getPollBatchSize() {
        return pollBatchSize;
      }

      public void setPollBatchSize(Integer pollBatchSize) {
        this.pollBatchSize = pollBatchSize;
      }

      public Integer getStallThresholdMinutes() {
        return stallThresholdMinutes;
      }

      public void setStallThresholdMinutes(Integer stallThresholdMinutes) {
        this.stallThresholdMinutes = stallThresholdMinutes;
      }
    }

    public static class ProviderConfig {
      @JsonProperty("storage-discovery-execution-timeout-seconds")
      private Integer storageDiscoveryExecutionTimeoutSeconds;

      public Integer getStorageDiscoveryExecutionTimeoutSeconds() {
        return storageDiscoveryExecutionTimeoutSeconds;
      }

      public void setStorageDiscoveryExecutionTimeoutSeconds(
          Integer storageDiscoveryExecutionTimeoutSeconds) {
        this.storageDiscoveryExecutionTimeoutSeconds = storageDiscoveryExecutionTimeoutSeconds;
      }
    }

    public static class ConsoleConfig {
      @JsonProperty("allowed-origins")
      private List<String> allowedOrigins;

      @JsonProperty("cleanup-interval-ms")
      private Integer cleanupIntervalMs;

      @JsonProperty("trust-all-hypervisor-tls")
      private Boolean trustAllHypervisorTls;

      public List<String> getAllowedOrigins() {
        return allowedOrigins;
      }

      public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
      }

      public Integer getCleanupIntervalMs() {
        return cleanupIntervalMs;
      }

      public void setCleanupIntervalMs(Integer cleanupIntervalMs) {
        this.cleanupIntervalMs = cleanupIntervalMs;
      }

      public Boolean getTrustAllHypervisorTls() {
        return trustAllHypervisorTls;
      }

      public void setTrustAllHypervisorTls(Boolean trustAllHypervisorTls) {
        this.trustAllHypervisorTls = trustAllHypervisorTls;
      }
    }
  }
}
