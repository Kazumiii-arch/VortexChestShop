plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "1.9.0" // Keep if you use Kotlin, otherwise remove
}

// Project metadata
group = "com.vortex" // Changed from "com.kazumiii" to "com.vortex"
version = "1.0.0" // Keeping your plugin's established version

repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    
    // Corrected JitPack repository declaration
    maven {
        url = uri("https://jitpack.io/")
    }
    
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") } // For PlaceholderAPI
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") } // For Vault
    maven { url = uri("https://repo.dmulloy2.net/repository/public/") } // For ProtocolLib
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") } // For Paper API, if you switch from SpigotAPI
    maven { url = uri("https://repo.spigotmc.org/releases/") } // For SpigotMC releases, if needed for other plugins
}

dependencies {
    // Spigot API for Minecraft 1.20.1.
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")

    // DecentHolograms dependency from JitPack (as requested).
    compileOnly("com.github.decentsoftware-eu:decentholograms:2.9.3")

    // Other core dependencies for VortexChestShop
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.11.2")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0")

    // Lombok for boilerplate reduction
    compileOnly("org.projectlombok:lombok:1.18.28")
    annotationProcessor("org.projectlombok:lombok:1.18.28")

    // JUnit for testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

java {
    // Set the Java version to 17, which is required for Minecraft 1.17+ plugins.
    toolchain.languageVersion.set(JavaLanguageVersion.of(17)) 
}

tasks {
    shadowJar {
        archiveBaseName.set("VortexChestShop") // Changed from "GachaCrate" to "VortexChestShop"
        archiveClassifier.set("")
        archiveVersion.set(project.version.toString())
        
        // Relocate ProtocolLib to avoid conflicts
        relocate("com.comphenix.protocol", "com.vortex.vortexchestshop.libs.protocollib")
        // If DecentHolograms also shades dependencies that conflict, you might need to add relocations here too.
        // Example: relocate("eu.decentsoftware.holograms", "com.vortex.vortexchestshop.libs.decentholograms")
    }

    build {
        dependsOn(shadowJar)
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    // Configure test task
    test {
        useJUnitPlatform()
    }

    // Clean task to remove build directories (using modern syntax)
    clean {
        delete(layout.buildDirectory.get().asFile)
    }
}
