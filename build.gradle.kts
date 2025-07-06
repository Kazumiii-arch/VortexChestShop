// build.gradle.kts

plugins {
    java // Apply the Java plugin for basic Java compilation
    // The shadow plugin helps package our project into a distributable JAR file.
    id("com.github.johnrengelman.shadow") version "8.1.1" // For shading dependencies into your plugin JAR
    kotlin("jvm") version "1.9.0" // If you plan to use Kotlin in your project, otherwise remove this line
}

// Project metadata
group = "com.vortex" // Your company's package group
version = "1.0.0" // Matches the version specified in your plugin.yml

repositories {
    mavenCentral() // Standard Maven repository
    // The repository needed to download the Spigot API
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") } // SpigotMC repository for Spigot API
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") } // Sonatype for various dependencies (e.g., Vault)
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") } // PlaceholderAPI repository
    maven { url = uri("https://repo.dmulloy2.net/repository/public/") } // ProtocolLib repository
    maven { url = uri("https://jitpack.io") } // For certain libraries that might be on JitPack (e.g., custom APIs)
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") } // PaperMC repository for Paper API (if using Paper API)
}

dependencies {
    // Spigot API for Minecraft 1.20.1.
    // 'compileOnly' means it's needed for compiling but won't be included in the
    // final JAR, as the server provides it at runtime.
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT") // Updated to 1.20.1 as per your example
    // For PaperMC, you might use:
    // compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")

    // Vault API - for economy integration (soft dependency)
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")

    // PlaceholderAPI - for dynamic text in displays (soft dependency)
    compileOnly("me.clip:placeholderapi:2.11.2")

    // ProtocolLib - for advanced packet manipulation (soft dependency, highly recommended for floating items)
    compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0") // Check for the latest compatible version

    // HolographicDisplays - for floating text (soft dependency)
    // Note: The artifact ID might vary slightly depending on the specific fork/version.
    // The one below is common for the modern HolographicDisplays.
    compileOnly("com.github.Decathlon-2.0:HolographicDisplays:3.0.0") // Check for the latest compatible version or alternative

    // Lombok - useful for reducing boilerplate code (optional, but common)
    compileOnly("org.projectlombok:lombok:1.18.28")
    annotationProcessor("org.projectlombok:lombok:1.18.28")

    // JUnit for testing (optional, but good practice)
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

java {
    // Set the Java version to 17, which is required for Minecraft 1.17+ plugins.
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    // Configure the shadowJar task to produce our final plugin file.
    shadowJar {
        archiveBaseName.set("VortexChestShop") // Set the base name of the output JAR file
        archiveClassifier.set("") // This removes the default classifier (e.g., '-all') from the JAR name.
        archiveVersion.set(project.version.toString()) // Use the project's version in the JAR name
        
        // Relocate ProtocolLib to avoid conflicts with other plugins that might use it.
        // This changes the package name of ProtocolLib classes inside your plugin's JAR.
        relocate("com.comphenix.protocol", "com.vortex.vortexchestshop.libs.protocollib")
        // Add other relocations here if you shade other libraries that might conflict
    }

    // Make the main 'build' task dependent on the 'shadowJar' task.
    // This ensures that running 'gradle build' will create the final fat JAR.
    build {
        dependsOn(shadowJar)
    }
    
    // Ensure the project uses UTF-8 encoding for all Java files for compatibility.
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    // Configure test task
    test {
        useJUnitPlatform()
    }

    // Clean task to remove build directories
    clean {
        delete(rootProject.buildDir)
    }
}
