plugins {
    `java-library`
}

dependencies {
    api(libs.spring.boot.starter.data.jpa)
    api(project(":libs:core-spi"))
    compileOnly(libs.swagger.annotations)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.vladmihalcea.hibernate.types)
    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-commons"))
    implementation(libs.jackson.databind)
}
