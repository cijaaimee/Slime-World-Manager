/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

dependencies {
    compileOnly("com.github.luben:zstd-jni:1.5.2-1")
    compileOnly("org.spigotmc:spigot-api:1.13-R0.1-SNAPSHOT")
    compileOnly(project(":slimeworldmanager-api"))
}

version = "3.0.0"

dependencies {
    implementation("com.flowpowered:flow-nbt:2.0.0")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("")
    archiveFileName.set("${project.name}-${version}.jar")
}

publishing {
    publications {
        create<MavenPublication>("slimeworldmanager-nms-common") {
            groupId = "net.redeforce.slimeworldmanager"
            artifactId = "slimeworldmanager-nms-common"
            version = "${project.version}-SNAPSHOT"

            artifact(tasks["shadowJar"])
        }
    }
}
