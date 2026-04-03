plugins { `java-library` }

dependencies {
    implementation(libs.libvirt)
    runtimeOnly(libs.jna)
    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-provider"))
    implementation(project(":libs:core-persistence"))
    implementation(project(":libs:core-commons"))
    implementation(libs.slf4j.api)
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test:4.0.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
