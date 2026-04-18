plugins {
    `java-library`
    id("maven-publish")
}

allprojects {
    group = "com.krito.muxon"
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
            url = uri("https://maven.pkg.github.com/krito/muxon-core")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
