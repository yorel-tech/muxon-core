plugins { `java-library` }

dependencies {
    api(libs.jackson.databind)
    implementation(libs.jackson.annotations)

    testImplementation("org.junit.jupiter:junit-jupiter:5.14.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt())) }
}

tasks.test {
    useJUnitPlatform()
}
