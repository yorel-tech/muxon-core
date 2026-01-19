plugins { alias(libs.plugins.quarkus) }

dependencies {
    implementation(project(":libs:core-commons"))
    implementation(enforcedPlatform(libs.quarkus.bom))
    implementation(libs.quarkus.kafka.client)
    implementation(libs.quarkus.opentelemetry)
    implementation(libs.quarkus.arc)
    implementation(project(":libs:core-persistence"))
    implementation(project(":libs:core-spi"))
    implementation(project(":libs:core-provider"))
}

tasks.register("runDev") {
    dependsOn("quarkusDev")
}
