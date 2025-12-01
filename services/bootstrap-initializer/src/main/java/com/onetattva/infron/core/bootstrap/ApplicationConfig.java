package com.onetattva.infron.core.bootstrap;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Configuration class representing the application.yaml structure
 */
public class ApplicationConfig {

    private ServerConfig server;
    private ManagementConfig management;
    private SpringConfig spring;
    private SpringDocConfig springdoc;
    private LoggingConfig logging;
    private InfronConfig infron;

    // Getters and setters
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

    public InfronConfig getInfron() {
        return infron;
    }

    public void setInfron(InfronConfig infron) {
        this.infron = infron;
    }

    public static class ServerConfig {
        private int port;

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class ManagementConfig {
        private EndpointsConfig endpoints;

        public EndpointsConfig getEndpoints() {
            return endpoints;
        }

        public void setEndpoints(EndpointsConfig endpoints) {
            this.endpoints = endpoints;
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
    }

    public static class SpringConfig {
        private SpringApplicationConfig application;
        private DatasourceConfig datasource;
        private JpaConfig jpa;
        private SecurityConfig security;
        private FlywayConfig flyway;
        private DataConfig data;

        public SpringApplicationConfig getApplication() {
            return application;
        }

        public void setApplication(SpringApplicationConfig application) {
            this.application = application;
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

        public DataConfig getData() {
            return data;
        }

        public void setData(DataConfig data) {
            this.data = data;
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

            public HibernateConfig getHibernate() {
                return hibernate;
            }

            public void setHibernate(HibernateConfig hibernate) {
                this.hibernate = hibernate;
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
            private boolean enabled;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }

        public static class DataConfig {
            private RedisConfig redis;

            public RedisConfig getRedis() {
                return redis;
            }

            public void setRedis(RedisConfig redis) {
                this.redis = redis;
            }

            public static class RedisConfig {
                private String host;
                private String port;

                public String getHost() {
                    return host;
                }

                public void setHost(String host) {
                    this.host = host;
                }

                public String getPort() {
                    return port;
                }

                public void setPort(String port) {
                    this.port = port;
                }
            }
        }
    }

    public static class SpringDocConfig {
        @JsonProperty("writer-with-order-by-keys")
        private boolean writerWithOrderByKeys;
        @JsonProperty("swagger-ui")
        private SwaggerUiConfig swaggerUi;

        public boolean isWriterWithOrderByKeys() {
            return writerWithOrderByKeys;
        }

        public void setWriterWithOrderByKeys(boolean writerWithOrderByKeys) {
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
            private boolean tryItOutEnabled;

            public boolean isTryItOutEnabled() {
                return tryItOutEnabled;
            }

            public void setTryItOutEnabled(boolean tryItOutEnabled) {
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

            public String getConsole() {
                return console;
            }

            public void setConsole(String console) {
                this.console = console;
            }
        }
    }

    public static class InfronConfig {
        private String instanceName;
        private int instanceId;
        private SystemConfig system;
        private List<TenantConfig> tenants;

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
    }
}
