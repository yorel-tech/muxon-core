plugins { `java-library` }

dependencies {
    implementation(libs.libvirt)
    runtimeOnly(libs.jna)
    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-provider"))
    implementation(libs.slf4j.api)
}
