plugins { `java-library` }

dependencies {
    implementation(libs.libvirt)
    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-provider"))
    implementation(libs.slf4j.api)
}
