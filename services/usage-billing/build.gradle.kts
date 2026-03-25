plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":libs:core-commons"))
    
    // Spring Boot starters
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    
    // Development tools
    developmentOnly(libs.spring.boot.devtools)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}
