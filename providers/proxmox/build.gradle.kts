plugins { `java-library` }

dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.pve4j)
    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-provider"))
    implementation(project(":libs:core-persistence"))
    implementation(project(":libs:core-commons"))
    implementation(libs.slf4j.api)
}