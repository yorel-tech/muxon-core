plugins {
    alias(libs.plugins.spring.boot)
}

import org.gradle.api.tasks.compile.JavaCompile

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

dependencies {
    implementation(libs.bundles.spring.boot.starter)
    implementation(libs.bundles.spring.boot.jdbc)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.caffeine)
    compileOnly(libs.swagger.annotations)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.jackson.annotations)
    implementation(libs.aws.s3)
    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-auth"))
    implementation(project(":libs:core-commons"))
    implementation(project(":libs:core-persistence"))
    implementation(project(":libs:core-provider"))
    implementation(project(":libs:core-proto"))
    implementation(project(":services:auth-api"))

    // gRPC client (to call the orchestrator's workflow services)
    implementation("net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE")
    implementation("io.grpc:grpc-netty-shaded:1.69.1")
}

tasks.jar {
    exclude("initial-config.yaml")
}
