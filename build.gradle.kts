plugins {
    id("fabric-loom") version "1.14-SNAPSHOT"
}

group = "com.example"
version = "0.1.0"

base {
    archivesName.set("anchor-addon")
}

repositories {
    mavenCentral()
    maven("https://maven.meteordev.org/releases")
    maven("https://maven.meteordev.org/snapshots")
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.11")
    mappings("net.fabricmc:yarn:1.21.11+build.1:v2")
    modImplementation("net.fabricmc:fabric-loader:0.18.1")
    modImplementation("meteordevelopment:meteor-client:1.21.11-SNAPSHOT")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.release.set(21)
    options.encoding = "UTF-8"
}
