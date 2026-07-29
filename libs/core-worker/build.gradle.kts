plugins {
    `java-library`
}

dependencies {
    api(project(":libs:core-provider"))
    api(project(":libs:core-persistence"))
    api(project(":libs:core-customization"))

    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-commons"))
    implementation(project(":providers:mock"))
    implementation(project(":providers:libvirt"))
    implementation(project(":providers:proxmox"))

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    implementation(libs.bundles.spring.boot.starter)

    // gRPC client for plugin resource dispatch (PluginGrpcChannelFactory)
    implementation(platform(libs.spring.grpc.dependencies))
    implementation(libs.spring.grpc.client.spring.boot.starter) {
        exclude(group = "io.grpc", module = "grpc-netty")
    }
    implementation(libs.grpc.netty.shaded)

    testImplementation("org.springframework.boot:spring-boot-starter-test:4.0.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

