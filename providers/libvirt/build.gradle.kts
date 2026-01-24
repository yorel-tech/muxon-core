plugins { `java-library` }

dependencies {
    implementation(libs.libvirt)
    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-provider"))
    implementation(project(":libs:core-persistence"))
    implementation(libs.slf4j.api)
}
