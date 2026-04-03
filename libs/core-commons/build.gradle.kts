plugins { `java-library` }

dependencies {
    api(libs.spring.web)
    implementation(libs.jackson.databind)
    implementation(libs.fasterxml.uuid)
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
