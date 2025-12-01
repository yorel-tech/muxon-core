plugins {
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    `java`
}

dependencies {
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.postgresql)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)
    implementation(libs.jackson.databind)
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.0")
    implementation(project(":libs:core-persistence"))
    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-commons"))
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.onetattva.infron.bootstrap.BootstrapApplication"
    }
}
