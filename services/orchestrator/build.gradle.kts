plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":libs:core-commons"))
    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-persistence"))
    implementation(project(":libs:core-spi"))
    implementation(project(":libs:core-provider"))
    
    // Spring Boot starters
    implementation(libs.bundles.spring.boot.starter)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.redis)
    
    // gRPC
    implementation(libs.grpc.spring.boot.starter)
    
    // OpenTelemetry & Micrometer
    implementation(libs.micrometer.tracing.bridge.otel)
    implementation(libs.opentelemetry.exporter.otlp)
    implementation(libs.micrometer.prometheus)
    
    // Development tools
    developmentOnly(libs.spring.boot.devtools)
}

tasks.register("runDev") {
    dependsOn("bootRun")
}
