package com.sal.muxon.bootstrap;

import com.sal.muxon.api.enums.BootstrapStatus;
import com.sal.muxon.initializer.config.BootstrapConfig;
import com.sal.muxon.initializer.service.CoreInitializerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
@ComponentScan(basePackages = {"com.sal.muxon.initializer"})
@EntityScan(basePackages = "com.sal.muxon.db.model")
@EnableJpaRepositories(basePackages = "com.sal.muxon.db.repository")
public class BootstrapApplication implements CommandLineRunner {

    @Autowired
    private CoreInitializerService coreInitializerService;

    public static void main(String[] args) throws IOException {
        String configPath = null;
        String outputFolder = null;
        String passphrasePath = null;
        String oidcSecretPath = null;
        String dbPasswordPath = null;

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
                case "--muxon-passphrase":
                    if (i + 1 < args.length) {
                        passphrasePath = args[++i];
                    } else {
                        System.out.println("Missing value for --muxon-passphrase");
                        printUsageAndExit();
                    }
                    break;
                case "--muxon-oidc-secret":
                    if (i + 1 < args.length) {
                        oidcSecretPath = args[++i];
                    } else {
                        System.out.println("Missing value for --muxon-oidc-secret");
                        printUsageAndExit();
                    }
                    break;
                case "--muxon-db-password":
                    if (i + 1 < args.length) {
                        dbPasswordPath = args[++i];
                    } else {
                        System.out.println("Missing value for --muxon-db-password");
                        printUsageAndExit();
                    }
                    break;
                default:
                    System.out.println("Unknown argument: " + args[i]);
                    printUsageAndExit();
            }
        }

        if (configPath == null || outputFolder == null || passphrasePath == null || oidcSecretPath == null || dbPasswordPath == null) {
            System.out.println("All arguments are required:");
            printUsageAndExit();
        }

        BootstrapConfig config;
        try {
            config = CoreInitializerService.loadConfig(configPath);
        } catch (Exception e) {
            System.err.println("Failed to load initial config: " + e.getMessage());
            System.exit(1);
            return;
        }

        String dbPassword = Files.readString(Paths.get(dbPasswordPath)).trim();

        // Set system properties for Spring Boot DataSource configuration
        System.setProperty("spring.datasource.url", config.getMuxon().getDatasource().getUrl());
        System.setProperty("spring.datasource.username", config.getMuxon().getDatasource().getUsername());
        System.setProperty("spring.datasource.password", dbPassword);
        System.setProperty("spring.datasource.driver-class-name", config.getMuxon().getDatasource().getDriverClassName());

        // Disable web application type since we don't need a web server
        System.setProperty("spring.main.web-application-type", "none");

        // Start Spring Boot application with arguments
        SpringApplication app = new SpringApplication(BootstrapApplication.class);
        app.run("--configPath=" + configPath, "--outputFolder=" + outputFolder, "--passphrasePath=" + passphrasePath, "--oidcSecretPath=" + oidcSecretPath, "--dbPasswordPath=" + dbPasswordPath);
    }

    private static void printUsageAndExit() {
        System.out.println("Usage: java -jar muxon-initializer.jar --initial-config <path> --output-folder <path> --muxon-passphrase <path> --muxon-oidc-secret <path> --muxon-db-password <path>");
        System.exit(1);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting bootstrap initialization");

        // Parse arguments from the command line args passed to run
        String configPath = null;
        String outputFolder = null;
        String passphrasePath = null;
        String oidcSecretPath = null;
        String dbPasswordPath = null;

        for (String arg : args) {
            if (arg.startsWith("--configPath=")) {
                configPath = arg.substring("--configPath=".length());
            } else if (arg.startsWith("--outputFolder=")) {
                outputFolder = arg.substring("--outputFolder=".length());
            } else if (arg.startsWith("--passphrasePath=")) {
                passphrasePath = arg.substring("--passphrasePath=".length());
            } else if (arg.startsWith("--oidcSecretPath=")) {
                oidcSecretPath = arg.substring("--oidcSecretPath=".length());
            } else if (arg.startsWith("--dbPasswordPath=")) {
                dbPasswordPath = arg.substring("--dbPasswordPath=".length());
            }
        }

        if (configPath == null || outputFolder == null || passphrasePath == null || oidcSecretPath == null || dbPasswordPath == null) {
            throw new IllegalArgumentException("Missing required arguments: configPath, outputFolder, passphrasePath, oidcSecretPath, dbPasswordPath");
        }

        boolean lockAcquired = false;
        try {
            lockAcquired = coreInitializerService.acquireLock();
            if (!lockAcquired) {
                throw new RuntimeException("Failed to acquire bootstrap advisory lock within timeout");
            }

            coreInitializerService.performOssInitialization(
                    configPath,
                    outputFolder,
                    passphrasePath,
                    oidcSecretPath,
                    dbPasswordPath
            );
            System.out.println("Bootstrap initialization completed");

        } catch (Exception e) {
            System.err.println("Error during bootstrap initialization:" + e.getMessage());
            throw e;
        } finally {
            if (lockAcquired) {
                coreInitializerService.releaseLock();
            }
        }
        System.exit(0);
    }
}
