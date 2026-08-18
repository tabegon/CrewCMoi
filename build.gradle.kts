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

    // Dépôts nécessaires pour l'API ItemsAdder (CustomStack) et sa dépendance LoneLibs
    maven("https://jitpack.io")
    maven("https://www.matteodev.it/spigot/public/maven/")

    // Dépôt PlaceholderAPI (BountyPlaceholderExpansion)
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")

    // Driver JDBC SQLite, embarqué dans le jar final via shadow
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")

    // API ItemsAdder (classe CustomStack, etc.) + sa dépendance LoneLibs.
    // "compileOnly" : ItemsAdder est installé séparément sur le serveur (softdepend),
    // pas besoin de l'embarquer dans le jar final.
    compileOnly("com.github.LoneDev6:api-itemsadder:3.6.1")
    compileOnly("dev.lone:LoneLibs:1.0.58")

    // API Vault (interface net.milkbowl.vault.economy.Economy, utilisée par
    // VaultEconomyProvider). "compileOnly" : Vault est installé séparément sur le
    // serveur (softdepend), pas besoin de l'embarquer dans le jar final.
    // Publiée sur jitpack.io (déjà ajouté au bloc repositories ci-dessus).
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")

    // API PlaceholderAPI (classe PlaceholderExpansion, utilisée par
    // BountyPlaceholderExpansion). "compileOnly" : PlaceholderAPI est installé
    // séparément sur le serveur (softdepend), pas besoin de l'embarquer dans le jar final.
    compileOnly("me.clip:placeholderapi:2.11.6")
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