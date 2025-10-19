import org.gradle.jvm.tasks.Jar
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    `java-library`
    alias(libs.plugins.openapi.generator)
}

dependencies {
    // Generated Spring interfaces/models rely on these:
    api("org.springframework.boot:spring-boot-starter-validation:3.5.6") // jakarta.validation
    api("com.fasterxml.jackson.core:jackson-annotations:2.20")
    // ➜ Add these so ApiUtil compiles (do NOT leak them transitively)
    compileOnly("org.springframework:spring-web:6.1.9")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")
    // ➜ Swagger/OpenAPI annotations used by generated interfaces
    compileOnly("io.swagger.core.v3:swagger-annotations:2.2.22")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
    //withSourcesJar()
}
// Where the plugin will write generated sources
val openApiOutputDir = layout.buildDirectory.dir("generated/sources/openapi")

tasks.named<GenerateTask>("openApiGenerate") {
    generatorName.set("spring")
    inputSpec.set("${rootDir}/openapi/openapi.yaml")
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