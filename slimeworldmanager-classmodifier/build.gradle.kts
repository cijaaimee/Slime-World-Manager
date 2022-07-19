/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

plugins {
    application
}

version = "3.0.0"

repositories {
    maven("https://libraries.minecraft.net")
}

application {
    mainClass.set("org.gradle.sample.Main")
}

dependencies {
    implementation("org.javassist:javassist:3.25.0-GA")
    implementation("org.yaml:snakeyaml:1.26")
    compileOnly("com.mojang:datafixerupper:1.0.20")
    compileOnly("org.spigotmc:spigot-api:1.14.4-R0.1-SNAPSHOT")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    manifest {
        attributes["Premain-Class"] = "com.grinderwolf.swm.clsm.NMSTransformer"
    }

    archiveClassifier.set("")
    archiveFileName.set("${project.name}-${version}.jar")
}

publishing {
    publications {
        create<MavenPublication>("slimeworldmanager-classmodifier") {
            groupId = "net.redeforce.slimeworldmanager"
            artifactId = "slimeworldmanager-classmodifier"
            version = "${project.version}-SNAPSHOT"

            artifact(tasks["shadowJar"])
        }
    }
}
