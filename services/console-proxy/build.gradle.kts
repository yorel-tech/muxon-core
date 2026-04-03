plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":libs:core-persistence"))
    implementation(libs.spring.boot.starter.web)
    implementation("org.springframework.boot:spring-boot-starter-websocket:${libs.versions.spring.boot.get()}")
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.postgresql)
    implementation(libs.slf4j.api)
    testImplementation("org.springframework.boot:spring-boot-starter-test:4.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
