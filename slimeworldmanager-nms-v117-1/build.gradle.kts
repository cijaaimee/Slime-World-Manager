plugins {
    id("io.papermc.paperweight.userdev") version "1.3.5"
}

dependencies {
    paperDevBundle("1.17.1-R0.1-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.17.1-R0.1-SNAPSHOT")
    compileOnly(project(":slimeworldmanager-nms-common"))
    compileOnly(project(":slimeworldmanager-api"))
    compileOnly(project(":slimeworldmanager-classmodifierapi"))

    implementation("com.flowpowered:flow-nbt:2.0.2")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
}

description = "slimeworldmanager-nms-v117-1"
