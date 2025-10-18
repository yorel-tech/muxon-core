plugins { alias(libs.plugins.quarkus) }
dependencies {
    implementation(project(":libs:core-commons"))
    implementation(enforcedPlatform(libs.quarkus.bom))
    implementation(libs.quarkus.scheduler)
}
