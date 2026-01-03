plugins { `java-library` }

dependencies {
    api(libs.spring.web)
    implementation(libs.jackson.databind)
    implementation(libs.fasterxml.uuid)
}
