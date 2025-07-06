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
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") } // Spigot snapshots
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") } // Sonatype (Vault, etc.)
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") } // PlaceholderAPI
    maven { url = uri("https://repo.dmulloy2.net/repository/public/") } // ProtocolLib, and some HD versions
    maven { url = uri("https://jitpack.io") } // Keep JitPack in case it's needed for other dependencies
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") } // PaperMC

    // ADDED: SpigotMC releases repository - often hosts plugins like HolographicDisplays
    maven { url = uri("https://repo.spigotmc.org/releases/") }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")

    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.11.2")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0")

    // CHANGED: HolographicDisplays to a commonly available version on SpigotMC releases repo
    compileOnly("com.sainttx.holograms:Holograms:2.5.11") // This version is often more reliably found

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
        delete(rootProject.buildDir)
    }
}
