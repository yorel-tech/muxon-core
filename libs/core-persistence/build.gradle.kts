plugins {
    `java-library`
}

dependencies {
    api(libs.spring.boot.starter.data.jpa)
    api(project(":libs:core-provider"))
    compileOnly(libs.swagger.annotations)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.vladmihalcea.hibernate.types)
    implementation(project(":libs:core-api"))
    implementation(project(":libs:core-commons"))
    implementation(libs.jackson.databind)
}

tasks.register<JavaExec>("validateDbSchema") {
    description = "Verify db-schema.yaml covers all oss/ migration tables/views"
    group = "verification"

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.sal.muxon.db.schema.DbSchemaValidator")
    args(
        "${project.projectDir}/src/main/resources/db-schema.yaml",
        "${project.projectDir}/src/main/resources/db/migration/oss"
    )
}
