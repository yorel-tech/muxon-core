package com.sal.muxon.initializer.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class BootstrapConfig {

    private MuxonConfig muxon;

    public MuxonConfig getMuxon() {
        return muxon;
    }

    public void setMuxon(MuxonConfig muxon) {
        this.muxon = muxon;
    }

    public static class MuxonConfig {
        private String instanceName;
        private int instanceId;
        private SystemConfig system;
        private List<TenantConfig> tenants;
        private DatasourceConfig datasource;

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

        public DatasourceConfig getDatasource() {
            return datasource;
        }

        public void setDatasource(DatasourceConfig datasource) {
            this.datasource = datasource;
        }
    }

    public static class SystemConfig {
        private String systemAdminUserName;
        private IdpConfig idp;

        public String getSystemAdminUserName() {
            return systemAdminUserName;
        }

        public void setSystemAdminUserName(String systemAdminUserName) {
            this.systemAdminUserName = systemAdminUserName;
        }

        public IdpConfig getIdp() {
            return idp;
        }

        public void setIdp(IdpConfig idp) {
            this.idp = idp;
        }
    }

    public static class IdpConfig {
        private String name;
        private String protocol;
        private Map<String, String> metadata;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata;
        }
    }

    public static class TenantConfig {
        private String name;
        private String idpName;
        private String tenantAdminUserName;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIdpName() {
            return idpName;
        }

        public void setIdpName(String idpName) {
            this.idpName = idpName;
        }

        public String getTenantAdminUserName() {
            return tenantAdminUserName;
        }

        public void setTenantAdminUserName(String tenantAdminUserName) {
            this.tenantAdminUserName = tenantAdminUserName;
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
}

