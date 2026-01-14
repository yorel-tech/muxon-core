plugins {
    java
}

group = "com.onetattva.infron"
version = "0.1.0"

dependencies {
    implementation(project(":libs:core-spi"))
    implementation(project(":libs:core-persistence"))
    implementation("jakarta.enterprise:jakarta.enterprise.cdi-api:3.0.0")
}
