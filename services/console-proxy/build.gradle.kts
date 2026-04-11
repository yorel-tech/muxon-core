plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":libs:core-commons"))
    implementation(project(":libs:core-persistence"))
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.web)
    implementation("org.springframework.boot:spring-boot-starter-websocket:${libs.versions.spring.boot.get()}")
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.postgresql)
    // Hypersistence JsonBinaryType (core-persistence entities) expects Jackson 2's ObjectMapper package.
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    implementation(libs.slf4j.api)
    testImplementation("org.springframework.boot:spring-boot-starter-test:4.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
