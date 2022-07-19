/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

dependencies {
    implementation(project(":slimeworldmanager-api"))
    implementation(project(":slimeworldmanager-nms-common"))

    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.google.guava:guava:26.0-jre")
    implementation("com.github.luben:zstd-jni:1.4.1-1")
    implementation("com.github.tomas-langer:chalk:1.0.2")
    implementation("com.flowpowered:flow-nbt:2.0.0")
}

version = "3.0.0"

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("")
    archiveFileName.set("${project.name}-${version}.jar")
}

publishing {
    publications {
        create<MavenPublication>("slimeworldmanager-importer") {
            groupId = "net.redeforce.slimeworldmanager"
            artifactId = "slimeworldmanager-importer"
            version = "${project.version}-SNAPSHOT"

            artifact(tasks["shadowJar"])
        }
    }
}
