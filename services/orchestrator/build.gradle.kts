plugins { alias(libs.plugins.quarkus) }

dependencies {
    implementation(project(":libs:core-commons"))
    implementation(enforcedPlatform(libs.quarkus.bom))
    implementation(libs.quarkus.opentelemetry)
    implementation(libs.quarkus.arc)
    implementation(libs.quarkus.spring.di)
    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-persistence"))
    implementation(project(":libs:core-spi"))
    implementation(project(":libs:core-provider"))
}

tasks.register("runDev") {
    dependsOn("quarkusDev")
}
