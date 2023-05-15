/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.net.URI

plugins {
    id("java")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "com.grinderwolf"
version = "3.0.0"
java.sourceCompatibility = JavaVersion.VERSION_11

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "com.github.johnrengelman.shadow")

    repositories {
        mavenLocal()
        mavenCentral()

        maven(getNexusRepository("snapshots/"))
        maven(getNexusRepository("releases/"))

        maven("https://jitpack.io")
        maven("https://repo.glaremasters.me/repository/concuncan/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.maven.apache.org/maven2/")
        maven("https://repo.codemc.org/repository/maven-public")
        maven("https://libraries.minecraft.net")
    }

    dependencies {
        compileOnly("org.projectlombok:lombok:1.18.26")
        annotationProcessor("org.projectlombok:lombok:1.18.26")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    publishing {
        repositories {
            maven(getNexusRepository("snapshots/"))
        }
    }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    archiveFileName.set("${project.name}-${version}.jar")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

publishing {
    repositories {
        maven(getNexusRepository("snapshots/"))
    }

    publications {
        create<MavenPublication>("slimeworldmanager") {
            groupId = "net.redeforce.slimeworldmanager"
            artifactId = "slimeworldmanager"
            version = "${project.version}-SNAPSHOT"

            artifact(tasks["shadowJar"])
        }
    }
}

fun getNexusRepository(repository: String): (MavenArtifactRepository).() -> Unit = {
    name = project.findProperty("force.name") as String
    url = URI((project.findProperty("force.url") as String) + repository)

    isAllowInsecureProtocol = true

    credentials {
        username = project.findProperty("force.user") as String
        password = project.findProperty("force.password") as String
    }
}
