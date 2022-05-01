dependencies {
    implementation("com.flowpowered:flow-nbt:2.0.2")
    implementation("com.github.luben:zstd-jni:1.5.2-2")
    compileOnly("io.papermc.paper:paper-api:1.17.1-R0.1-SNAPSHOT")
    compileOnly(project(":slimeworldmanager-api"))
}

description = "slimeworldmanager-nms-common"
