plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "1.9.0" // Keep if you're using Kotlin; remove if not
}

// Project metadata
group = "com.vortex"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://jitpack.io/") }
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
    maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") } // For Paper, PlaceholderAPI, etc.
}

dependencies {
    // Spigot API for Minecraft 1.20.1
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")

    // Latest DecentHolograms requiring Java 21
    compileOnly("com.github.decentsoftware-eu:decentholograms:2.9.3")

    // Other dependencies
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.28")
    annotationProcessor("org.projectlombok:lombok:1.18.28")

    // JUnit for testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21)) 
}

tasks {
    shadowJar {
        archiveBaseName.set("VortexChestShop")
        archiveClassifier.set("")
        archiveVersion.set(project.version.toString())

        relocate("com.comphenix.protocol", "com.vortex.vortexchestshop.libs.protocollib")
        // Optional: Add more relocations if needed
    }

    build {
        dependsOn(shadowJar)
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    test {
        useJUnitPlatform()
    }

    clean {
        delete(layout.buildDirectory.get().asFile)
    }
}
