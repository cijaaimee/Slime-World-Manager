/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

plugins {
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
}

dependencies {
    implementation(project(":slimeworldmanager-api"))
    implementation(project(":slimeworldmanager-nms-common"))
    implementation(project(":slimeworldmanager-nms-v1_8_R3"))
    implementation("com.github.luben:zstd-jni:1.5.2-1")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.mongodb:mongo-java-driver:3.12.11")
    implementation("org.spongepowered:configurate-yaml:3.7-SNAPSHOT")
    implementation("com.flowpowered:flow-nbt:2.0.0")

    compileOnly("org.spigotmc:spigot-api:1.13-R0.1-SNAPSHOT")
}

version = "3.0.1"

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("")
    archiveFileName.set("${project.name}-${version}.jar")
}

bukkit {
    name = "SlimeWorldManager"
    main = "com.grinderwolf.swm.plugin.SWMPlugin"
    description = "Slime World Manager is a resource that implements the Slime Region Format, " +
            "designed by the Hypixel Team to load and save worlds more efficiently."
    prefix = "SWM"
    apiVersion = "1.8"
    version = "3.0.1"
    website = "redeforce.net"
    authors = listOf("Pedro Aguiar", "Grinderwolf")
    commands {
        register("swm") {
            description = "Slime World Manager command helper"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("slimeworldmanager-plugin") {
            groupId = "net.redeforce.slimeworldmanager"
            artifactId = "slimeworldmanager-plugin"
            version = "${project.version}-SNAPSHOT"

            artifact(tasks["shadowJar"])
        }
    }
}

