plugins {
    kotlin("jvm") version "2.1.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test-junit5"))

    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("org.postgresql:postgresql:42.7.3")

    implementation("io.ktor:ktor-server-core-jvm:2.3.7")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-jackson:2.3.7")

    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.quartz-scheduler:quartz:2.3.2")

    // ✅ WASM runtime (correct artifact)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
