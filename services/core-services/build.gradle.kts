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
    implementation(libs.spring.boot.starter.data.redis)
    compileOnly(libs.swagger.annotations)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.jackson.annotations)
    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-auth"))
    implementation(project(":libs:core-commons"))
    implementation(project(":libs:core-persistence"))
    implementation(project(":services:auth-api"))
}

tasks.jar {
    exclude("initial-config.yaml")
}
