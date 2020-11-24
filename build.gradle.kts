plugins {
    kotlin("jvm") version "1.3.72"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("de.undercouch.download") version "4.1.1"
}

group = "com.github.pikokr"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://papermc.io/repo/repository/maven-public/")
    maven(url = "https://jitpack.io")
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly("com.destroystokyo.paper:paper-api:1.16.4-R0.1-SNAPSHOT")
    implementation("com.github.noonmaru:kommand:0.6.3")
}

tasks {
    processResources {
        filesMatching("**/*.yml") {
            expand(project.properties)
        }
    }

    shadowJar {
        archiveBaseName.set(project.property("pluginName").toString())
        archiveVersion.set("")
        archiveClassifier.set("")
    }

    create<Copy>("docker") {
        from(shadowJar)
        into(File(".docker/plugins"))
    }
}
