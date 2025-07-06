// build.gradle.kts

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "1.9.0" // Keep if you use Kotlin, otherwise remove
}

group = "com.vortex"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
    maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://repo.spigotmc.org/releases/") }

    // ADDED: Repository for DecentHolograms
    maven { url = uri("https://repo.decentholograms.com/snapshots") } // For snapshots, if needed
    maven { url = uri("https://repo.decentholograms.com/releases") }  // For stable releases
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")

    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.11.2")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0")

    // CHANGED: Replaced HolographicDisplays with DecentHolograms
    compileOnly("eu.decentsoftware.holograms:decentholograms:2.8.6") // Using a recent stable version.
                                                                    // Check DecentHolograms' GitHub for latest.

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
        // If DecentHolograms has shaded dependencies that conflict, you might need to relocate them too.
        // Example: relocate("some.conflicting.package", "com.vortex.vortexchestshop.libs.decentholograms.some.conflicting.package")
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
        delete(rootProject.buildDir)
    }
}
