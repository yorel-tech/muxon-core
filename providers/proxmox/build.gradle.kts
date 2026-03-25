plugins { `java-library` }

dependencies {
    implementation(libs.pve4j)
    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-provider"))
    implementation(project(":libs:core-persistence"))
    implementation(project(":services:core-services"))
    implementation(libs.slf4j.api)
}