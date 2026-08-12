plugins {
    `java-library`
    id("maven-publish")
    id("com.diffplug.spotless") version "7.0.2" apply false
}

allprojects {
    group = "com.yorel.muxon"
    version = "0.1.0"

    repositories {
        mavenCentral()
        maven { url = uri("https://libvirt.org/maven2") }
    }

    tasks.withType<JavaCompile> {
        options.release.set(25)
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "checkstyle")

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("src/**/*.java")
            googleJavaFormat("1.35.0")
            removeUnusedImports()
            licenseHeaderFile(rootProject.file("config/spotless/license-header.java"))
        }
    }

    configure<CheckstyleExtension> {
        toolVersion = "10.21.1"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        isIgnoreFailures = false
        maxWarnings = 0
    }

    tasks.withType<Checkstyle>().configureEach {
        val hasSources = file("src/main/java").exists() || file("src/test/java").exists()
        onlyIf { hasSources }
        // OpenAPI Generator output is not Checkstyle-clean; keep linting hand-written sources only.
        exclude("**/build/generated/**")
    }

    // Publish leaf modules so Enterprise can resolve Core from mavenLocal / GH packages.
    // Skip aggregators (no build script) and integration-tests.
    val isPublishableLeaf =
        name != "integration-tests" &&
            (file("build.gradle.kts").exists() || file("build.gradle").exists())
    if (isPublishableLeaf) {
        apply(plugin = "maven-publish")
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                }
            }
            repositories {
                maven {
                    name = "github"
                    url = uri("https://maven.pkg.github.com/yorel-tech/muxon-core")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR")
                        password = System.getenv("GITHUB_TOKEN")
                    }
                }
            }
        }
    }
}
