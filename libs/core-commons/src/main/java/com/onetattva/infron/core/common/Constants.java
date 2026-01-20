package com.onetattva.infron.core.common;

public class Constants {
    public static final String SYSTEM_ID = "215012d9-8b1e-5dc5-b54f-89022875fe1e";
    public static final String DEFAULT_IDP_ID = "62083d54-cb8c-521f-9374-65e9f21c8991";
    public static final String NAMESPACE_ID = "696e6672-6f6e-636f-7265-111111111111";

    public static final long BOOTSTRAP_ADVISORY_LOCK_KEY = 0x696e66726f6eL; // 'infron' in hex
    public static final int BOOTSTRAP_TIMEOUT_SECONDS = 60;
    public static final String BOOTSTRAP_DONE_KEY = "bootstrap_done";
    private static final String ENCRYPTION_KEY_KEY = "encryption_key";

    // Default role names
    public static final String ROLE_SYSTEM_ADMIN = "system:admin";
    public static final String ROLE_TENANT_ADMIN = "tenant:admin";
    public static final String ROLE_WORKLOAD_OPERATOR = "workload:operator";
    public static final String ROLE_WORKLOAD_USER = "workload:user";

    public static final String MASKED_SECRET = "*****";
}
