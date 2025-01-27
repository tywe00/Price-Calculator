plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinx.serialization) // Serialization plugin
}

group = "com.example"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server Dependencies
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.content.negotiation) // Content Negotiation

    // Ktor Client Dependencies
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Logging
    implementation(libs.logback.classic)

    // Testing Dependencies
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.junit.jupiter)           // Use junit-jupiter
    testImplementation(libs.kotlin.test.junit5)      // Use kotlin-test-junit5
    testImplementation(libs.mockk)
    testImplementation(libs.ktor.client.mock)
}

tasks.test {
    useJUnitPlatform()
}