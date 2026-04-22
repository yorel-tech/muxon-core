plugins {
    `java-library`
}

dependencies {
    api(libs.spring.boot.starter.data.jpa)
    api(libs.spring.boot.starter.jdbc)
    implementation(libs.postgresql)
    api(libs.flyway.core)
    api(libs.flyway.postgres)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.yaml)

    implementation(project(":libs:core-persistence"))
    api(project(":libs:core-api"))
    api(project(":libs:core-commons"))
}

