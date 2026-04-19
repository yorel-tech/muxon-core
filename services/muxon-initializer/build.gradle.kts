plugins {
    alias(libs.plugins.spring.boot)
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
    implementation(libs.jackson.dataformat.yaml)
    implementation(project(":libs:core-persistence"))
    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-commons"))
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.krito.muxon.bootstrap.BootstrapApplication"
    }
}
