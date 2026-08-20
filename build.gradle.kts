plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.5"
}

group = "fr.crewcmoi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")

    maven("https://jitpack.io")
    maven("https://www.matteodev.it/spigot/public/maven/")

    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")

    maven("https://maven.citizensnpcs.co/repo")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")

    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.6.1")
    compileOnly("dev.lone:LoneLibs:1.0.58")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("me.clip:placeholderapi:2.11.6")

    compileOnly("net.citizensnpcs:citizens-main:2.0.35-SNAPSHOT") {
        exclude(group = "*", module = "*")
    }
}

val targetJavaVersion = 21
tasks.withType<JavaCompile> {
    options.release.set(targetJavaVersion)
    options.encoding = "UTF-8"
}

tasks.processResources {
    val props = mapOf("version" to version)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}