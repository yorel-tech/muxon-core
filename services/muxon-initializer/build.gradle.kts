plugins {
    alias(libs.plugins.spring.boot)
    `java`
}

dependencies {
    implementation(project(":libs:core-initializer"))
    implementation(libs.spring.boot.starter.web)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.krito.muxon.bootstrap.BootstrapApplication"
    }
}
