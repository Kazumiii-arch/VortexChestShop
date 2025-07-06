// build.gradle.kts

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "1.9.0" // Keep if you use Kotlin, otherwise remove
}

group = "com.vortex"
version = "1.0.0"

repositories {
    // === HIGH PRIORITY REPOSITORIES FOR SPECIFIC PLUGINS ===
    // Official repository for PlaceholderAPI
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
    // Official repository for DecentHolograms (releases)
    maven { url = uri("https://repo.decentholograms.com/releases") }
    // Official repository for DecentHolograms (snapshots, less stable but might be needed for latest features)
    maven { url = uri("https://repo.decentholograms.com/snapshots") }
    // Repository for ProtocolLib (dmulloy2) and sometimes other common plugins
    maven { url = uri("https://repo.dmulloy2.net/repository/public/") }

    // === GENERAL REPOSITORIES ===
    mavenCentral() // Standard Maven repository
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") } // SpigotMC snapshots
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") } // Sonatype (Vault, etc.)
    maven { url = uri("https://jitpack.io") } // Keep JitPack in case it's needed for other dependencies
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") } // PaperMC (if using Paper API)
    maven { url = uri("https://repo.spigotmc.org/releases/") } // SpigotMC releases
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT") // Target Minecraft 1.20.1

    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.11.2")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0")
    compileOnly("eu.decentsoftware.holograms:decentholograms:2.8.6") // DecentHolograms dependency

    compileOnly("org.projectlombok:lombok:1.18.28")
    annotationProcessor("org.projectlombok:lombok:1.18.28")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    shadowJar {
        archiveBaseName.set("VortexChestShop")
        archiveClassifier.set("")
        archiveVersion.set(project.version.toString())
        relocate("com.comphenix.protocol", "com.vortex.vortexchestshop.libs.protocollib")
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
        // Updated to use layout.buildDirectory.get().asFile to avoid deprecation warning
        delete(layout.buildDirectory.get().asFile)
    }
}
