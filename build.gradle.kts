plugins {
    kotlin("jvm") version "1.9.10"
    application
}

group = "gg.mt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-tree:9.6")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}