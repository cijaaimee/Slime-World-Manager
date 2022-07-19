/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

version = "3.0.0"

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("")
    archiveFileName.set("${project.name}-${version}.jar")
}

dependencies {
    implementation("com.flowpowered:flow-nbt:2.0.0")
}

publishing {
    publications {
        create<MavenPublication>("slimeworldmanager-api") {
            groupId = "net.redeforce.slimeworldmanager"
            artifactId = "slimeworldmanager-api"
            version = "${project.version}-SNAPSHOT"

            artifact(tasks["shadowJar"])
        }
    }
}
