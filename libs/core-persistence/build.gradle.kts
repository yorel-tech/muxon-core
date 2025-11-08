plugins {
    `java-library`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-jpa:${libs.versions.spring-boot.get()}")
}
