plugins {
    `java-library`
}

dependencies {
    api(project(":libs:core-provider"))
    api(project(":libs:core-persistence"))

    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-commons"))
    implementation(project(":providers:mock"))
    implementation(project(":providers:libvirt"))
    implementation(project(":providers:proxmox"))

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    implementation(libs.bundles.spring.boot.starter)

    testImplementation("org.springframework.boot:spring-boot-starter-test:4.0.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

