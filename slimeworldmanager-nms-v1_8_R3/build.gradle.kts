/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

dependencies {
    implementation(project(":slimeworldmanager-nms-common"))
    implementation(project(":slimeworldmanager-api"))
    implementation(project(":slimeworldmanager-classmodifier"))
    implementation("com.flowpowered:flow-nbt:2.0.0")

    compileOnly("org.spigotmc:spigot:1.8.8-R0.1-SNAPSHOT")
}

version = "3.0.0"


