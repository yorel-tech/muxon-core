plugins {
    java
}

group = "com.yorel.muxon"
version = "0.1.0"

dependencies {
    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-provider"))
    implementation("jakarta.enterprise:jakarta.enterprise.cdi-api:3.0.0")
}
