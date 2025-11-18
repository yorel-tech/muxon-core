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
        "@redocly/cli",
        "lint",
        openApiInput.asFile.absolutePath,
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
            "skipDefaultInterface" to "true",
            "skipOperationExample" to "true",
            "generateBuilders" to "true",
            "serializableModel" to "true",
            "useTags" to "true",
            "dateLibrary" to "java8",
            "useSpringBoot3" to "true",
            "useJakartaEe" to "true",
            //"performBeanValidation" to "true",
            "useFullyQualifiedNames" to "true",
            "oas3" to "true",
            "openApiNullable" to "false",   // ← disables JsonNullable usage
            "openApiSpec" to "3.0.3"
        )
    )

    importMappings.set(mapOf(
        // ensure the generator imports the Jakarta Email annotation
        "Email" to "jakarta.validation.constraints.Email"
    ))
    typeMappings.set(mapOf(
        // tells the generator to consider Email a known type -> helps imports in some templates
        "Email" to "jakarta.validation.constraints.Email"
    ))
    // clean the output dir before regen
    doFirst { delete(openApiOutputDir) }
}

val generatedJavaDir = openApiOutputDir.map { it.file("src/main/java") }

//tasks.register("patchGeneratedEmailAnnotations") {
//    group = "openapi"
//    description = "Add jakarta.validation Email import to generated sources where missing"
//    dependsOn("openApiGenerate")
//    doLast {
//        val dir = generatedJavaDir.get().asFile
//        if (!dir.exists()) return@doLast
//
//        val javaFiles = fileTree(dir).matching { include("**/*.java") }.files
//        javaFiles.forEach { file ->
//            var text = file.readText()
//            val hasEmailAnnotation = text.contains("@Email") || text.contains("@jakarta.validation.constraints.Email")
//            if (!hasEmailAnnotation) return@forEach
//
//            // If the file already imports jakarta Email, nothing to do
//            if (text.contains("import jakarta.validation.constraints.Email;")) return@forEach
//
//            // If the file imports the old Hibernate Email, prefer jakarta (we will add jakarta import to disambiguate)
//            // Insert the jakarta import after package and existing imports
//            val packageRegex = Regex("""(^package\s+[\w\.]+;\s*)""", RegexOption.MULTILINE)
//            val importJakarta = "import jakarta.validation.constraints.Email;\n"
//            if (packageRegex.containsMatchIn(text)) {
//                // find the last import line to append after; otherwise insert after package line
//                val lastImportMatch = Regex("""(?s)(?:^|\n)import\s+[\w\.\*]+;\s*""").findAll(text).lastOrNull()
//                val insertPos =
//                    lastImportMatch?.range?.endInclusive?.plus(1) ?: (packageRegex.find(text)!!.range.endInclusive + 1)
//                text = text.substring(0, insertPos) + "\n" + importJakarta + text.substring(insertPos)
//            } else {
//                // No package line? just prepend import
//                text = importJakarta + text
//            }
//
//            // Also, if the file contains simple @Email usages *and* generator left some fully-qualified ones,
//            // the explicit import will disambiguate to Jakarta.
//            file.writeText(text)
//        }
//    }
//}

sourceSets.named("main") {
    java.srcDir(openApiOutputDir.map { it.dir("src/main/java") })
    resources.srcDir(openApiOutputDir.map { it.dir("src/main/resources") })
}
tasks.processResources { dependsOn("openApiGenerate") }
tasks.compileJava { dependsOn("openApiGenerate") }
//tasks.named<Jar>("sourcesJar") { dependsOn("openApiGenerate") }
tasks.clean { doFirst { delete(openApiOutputDir) } }
