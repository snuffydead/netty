plugins {
    kotlin("jvm") version "2.1.10"
}

group = "snuffy.connection"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.netty:netty-all:4.1.107.Final")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}