plugins { alias(libs.plugins.quarkus) }

dependencies {
    implementation(project(":libs:core-commons"))
    implementation(enforcedPlatform(libs.quarkus.bom))
    implementation(libs.quarkus.kafka.client)
    implementation(libs.quarkus.opentelemetry)
    implementation(libs.quarkus.arc)
}

tasks.register("runDev") {
    dependsOn("quarkusDev")
}
