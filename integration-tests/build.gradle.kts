plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(project(":libs:core-api"))
    testImplementation(project(":libs:core-persistence"))
    testImplementation(project(":services:core-services"))
    testCompileOnly(libs.swagger.annotations)
    testImplementation(libs.jackson.databind)
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.1")
    testImplementation("org.junit.platform:junit-platform-suite:1.10.0")
    testImplementation("org.junit.platform:junit-platform-suite-api")
    testImplementation("org.junit.platform:junit-platform-engine")
    testImplementation("org.testcontainers:testcontainers:1.21.4")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("io.rest-assured:rest-assured:6.0.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test:4.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
