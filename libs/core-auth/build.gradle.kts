plugins { `java-library` }
dependencies {
    api("org.springframework.boot:spring-boot-starter-oauth2-resource-server:3.3.2")
    implementation(project(":libs:core-commons"))
}
