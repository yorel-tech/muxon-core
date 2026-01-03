plugins {
    `java-library`
}

dependencies {
    compileOnly(libs.swagger.annotations)
    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-auth"))
    implementation(project(":libs:core-persistence"))
    implementation(project(":libs:core-commons"))
}
