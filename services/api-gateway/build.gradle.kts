plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dep.mgmt)
}

dependencies {
    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-auth"))
    implementation(project(":libs:core-commons"))
    implementation(project(":libs:core-persistence"))
    implementation(project(":services:core-services"))

    implementation(libs.bundles.spring.boot.starter)
    implementation(libs.bundles.spring.boot.jdbc)
    implementation(libs.micrometer.prometheus)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
