plugins {
    `java-library`
}

group = "com.yorel.muxon"
version = "0.1.0"

dependencies {
    api(project(":libs:core-api"))
    implementation("jakarta.enterprise:jakarta.enterprise.cdi-api:3.0.0")
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
    compileOnly(libs.swagger.annotations)
    annotationProcessor(libs.swagger.annotations)
}
