plugins {
    `java-library`
}

dependencies {
    api(libs.spring.boot.starter.data.jpa)
    implementation(libs.vladmihalcea.hibernate.types)
    implementation(project(":libs:core-api"))
}
