plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":libs:core-commons"))
    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-persistence"))
    implementation(project(":libs:core-provider"))
    implementation(project(":libs:core-proto"))

    // Provider modules
    implementation(project(":providers:mock"))
    implementation(project(":providers:libvirt"))
    implementation(project(":providers:proxmox"))

    // Spring Boot starters
    implementation(libs.bundles.spring.boot.starter)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.postgresql)

    // gRPC server (Spring gRPC + grpc-netty-shaded)
    implementation(platform(libs.spring.grpc.dependencies))
    implementation(libs.spring.grpc.server.spring.boot.starter) {
        exclude(group = "io.grpc", module = "grpc-netty")
    }
    implementation(libs.grpc.netty.shaded)
    
    // Development tools
    developmentOnly(libs.spring.boot.devtools)
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test:4.0.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.register("runDev") {
    dependsOn("bootRun")
}

tasks.test {
    useJUnitPlatform()
}
