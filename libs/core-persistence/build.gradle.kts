plugins {
    `java-library`
}

dependencies {
    api(libs.spring.boot.starter.data.jpa)
    compileOnly(libs.swagger.annotations)
    implementation(libs.vladmihalcea.hibernate.types)
    implementation(project(":libs:core-api"))
}
