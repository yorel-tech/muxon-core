plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(project(":libs:core-api"))
    testCompileOnly(libs.swagger.annotations)
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.1")
    testImplementation("org.junit.platform:junit-platform-suite:1.10.0")
    testImplementation("org.junit.platform:junit-platform-suite-api")
    testImplementation("org.junit.platform:junit-platform-engine")
    testImplementation("org.testcontainers:testcontainers:1.21.4")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("io.rest-assured:rest-assured:6.0.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
