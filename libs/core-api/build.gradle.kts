import org.gradle.jvm.tasks.Jar
import com.github.gradle.node.npm.task.NpmTask
import org.gradle.api.tasks.Exec
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    `java-library`
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.node.gradle)
}

dependencies {
    // Generated Spring interfaces/models rely on these:
    api(libs.spring.boot.validation) // jakarta.validation
    api(libs.jackson.annotations)
    // ➜ Add these so ApiUtil compiles (do NOT leak them transitively)
    compileOnly(libs.spring.web)
    compileOnly(libs.servlet.api)
    // ➜ Swagger/OpenAPI annotations used by generated interfaces
    compileOnly(libs.swagger.annotations)
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt())) }
    //withSourcesJar()
}
// Where the plugin will write generated sources
val openApiOutputDir = layout.buildDirectory.dir("generated/sources/openapi")
val openApiInput = layout.projectDirectory.file("${rootDir}/openapi/openapi.yaml")
val bundledOpenApi = layout.buildDirectory.file("${rootDir}/openapi/bundled.yaml")

node {
    download.set(true)
    version.set(libs.versions.nodejs.get())
    npmVersion.set(libs.versions.npm.get())
    workDir.set(layout.projectDirectory.dir(".gradle/nodejs"))
    nodeProjectDir.set(layout.projectDirectory)
}

// Install the redocly/openapi-cli in a reproducible way
val redoclyVersion = libs.versions.redocly.cli.get()
tasks.register<NpmTask>("npmInstallOpenapiCli") {
    description = "Install redocly openapi-cli locally for validation/bundling"
    args.set(listOf("install", "--no-save", "@redocly/cli@${redoclyVersion}")) // pin version
}

// Validate (lint) the spec
tasks.register<NpmTask>("validateOpenApi") {
    dependsOn("npmInstallOpenapiCli")
    group = "openapi"
    description = "Validate OpenAPI via @redocly/cli lint"
    // redocly returns non-zero on errors -> build fails
    // commandLine = listOf("exec", "--", "@redocly/cli", "lint", openApiInput.asFile.absolutePath)
    // npm exec will invoke installed package binary
    // args: npm exec -- <pkg> <subcommand> ...
    npmCommand = (listOf(
        "exec",
        "--",
        "@redocly/openapi-cli",
        "bundle",
        openApiInput.asFile.absolutePath,
        "--output",
        bundledOpenApi.get().asFile.absolutePath
    ))
    // declare inputs so Gradle can consider changes
    inputs.file(openApiInput)
    // no real output file for validate: we mark it as up-to-date when input hasn't changed
    outputs.upToDateWhen { false }
}

// Bundle into single file (optional but handy)
tasks.register<NpmTask>("bundleOpenApi") {
    group = "openapi"
    description = "Bundle OpenAPI files to a single YAML (npx @redocly/cli bundle)"
    dependsOn("validateOpenApi")
    npmCommand = listOf(
        "exec",
        "--",
        "@redocly/cli",
        "bundle",
        openApiInput.asFile.absolutePath,
        "--output",
        bundledOpenApi.get().asFile.absolutePath
    )
    // declare inputs so Gradle can consider changes
    inputs.file(openApiInput)
    outputs.file(bundledOpenApi)
}

tasks.named<GenerateTask>("openApiGenerate") {
    dependsOn("bundleOpenApi")
    generatorName.set("spring")
    description = "Generate client from bundled OpenAPI"
    inputSpec.set(bundledOpenApi.get().asFile.absolutePath)
    outputDir.set(openApiOutputDir.get().asFile.absolutePath)

    apiPackage.set("com.onetattva.infron.api")
    modelPackage.set("com.onetattva.infron.api.model")
    invokerPackage.set("com.onetattva.infron.api.invoker")

    // ✅ Correct options for Spring Boot 3 + Jakarta
    additionalProperties.set(
        mapOf(
            "interfaceOnly" to "true",
            "useTags" to "true",
            "dateLibrary" to "java8",
            "useSpringBoot3" to "true",
            "useJakartaEe" to "true",
            "performBeanValidation" to "true",
            "useFullyQualifiedNames" to "true",
            "openApiNullable" to "false"   // ← disables JsonNullable usage
        )
    )

    // clean the output dir before regen
    doFirst { delete(openApiOutputDir) }
}

sourceSets.named("main") {
    java.srcDir(openApiOutputDir.map { it.dir("src/main/java") })
    resources.srcDir(openApiOutputDir.map { it.dir("src/main/resources") })
}
tasks.processResources { dependsOn("openApiGenerate") }
tasks.compileJava { dependsOn("openApiGenerate") }
//tasks.named<Jar>("sourcesJar") { dependsOn("openApiGenerate") }
tasks.clean { doFirst { delete(openApiOutputDir) } }