plugins {
    id("java")
    alias(libs.plugins.micronaut.app)
}

micronaut {
    runtime("netty")
    processing {
        incremental(true)
        annotations("com.onetattva.infron.agent.*")
    }
}

dependencies {
    implementation(project(":libs:core-commons"))
}

application {
    mainClass.set("com.onetattva.infron.agent.AgentApp")
}
