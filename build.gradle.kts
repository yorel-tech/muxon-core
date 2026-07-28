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
        // Only enforce on files changed vs main so the first enablement does not rewrite the tree.
        ratchetFrom("origin/main")
        java {
            target("src/**/*.java")
            googleJavaFormat("1.25.2")
            licenseHeaderFile(rootProject.file("config/spotless/license-header.java"))
        }
    }

    configure<CheckstyleExtension> {
        toolVersion = "10.21.1"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        // Report-only until the tree is cleaned; Spotless ratchet covers new/changed files.
        isIgnoreFailures = true
        maxWarnings = Integer.MAX_VALUE
    }

    tasks.withType<Checkstyle>().configureEach {
        val hasSources = file("src/main/java").exists() || file("src/test/java").exists()
        onlyIf { hasSources }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {

        maven {
            name = "github"
            url = uri("https://maven.pkg.github.com/yorel/muxon-core")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
