package com.grinderwolf.swm.crlfixer;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class NMSClassModifier implements ClassFileTransformer {

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        instrumentation.addTransformer(new NMSClassModifier());
    }

    @Override
    public byte[] transform(ClassLoader classLoader, String className, Class<?> classBeingTransformed, ProtectionDomain protectionDomain, byte[] bytes) {
        if (className != null) {
            String fixedClassName = className.replace("/", ".");

            try {
                if (fixedClassName.endsWith("PlayerChunkMap")) {
                    return fixPlayerChunkMap(fixedClassName, bytes).toBytecode();
                }

                if (fixedClassName.endsWith(".MinecraftServer")) {
                    return fixMinecraftServer(fixedClassName, bytes).toBytecode();
                }
            } catch (NotFoundException ex) {
                System.err.println("Failed to override NMS methods from class " + fixedClassName + ". Is this server running spigot 1.14?");

                ex.printStackTrace();
            } catch (CannotCompileException | IOException ex) {
                System.err.println("Failed to override NMS methods from class " + fixedClassName + ". ");
                ex.printStackTrace();
            }
        }

        return null;
    }

    private CtClass fixPlayerChunkMap(String className, byte[] clazz) throws NotFoundException, CannotCompileException {
        ClassPool pool = ClassPool.getDefault();
        pool.appendClassPath(new ByteArrayClassPath(className, clazz));
        CtClass ctClass = pool.get(className);

        // Load chunk
        CtMethod[] loadChunkMethods = ctClass.getDeclaredMethods("f");

        for (CtMethod method : loadChunkMethods) {
            if (method.getParameterTypes().length == 0) {
                // There is another f() method which we're not interested in
                continue;
            }

            method.insertBefore("{ " +
                    "    java.util.concurrent.CompletableFuture chunk = com.grinderwolf.swm.crlfixer.CRLFixer.getFutureChunk($0.world, $1.x, $1.z);" +
                    "    if (chunk != null) { " +
                    "        return chunk;" +
                    "    }" +
                    "}");
        }

        // Save chunk
        CtMethod saveChunkMethod = ctClass.getDeclaredMethod("saveChunk");

        saveChunkMethod.insertBefore("{" +
                "    if (com.grinderwolf.swm.crlfixer.CRLFixer.saveChunk($0.world, $1)) {" +
                "        return false;" +
                "    }" +
                "}");

        return ctClass;
    }

    private CtClass fixMinecraftServer(String className, byte[] clazz) throws NotFoundException, CannotCompileException {
        ClassPool pool = ClassPool.getDefault();
        pool.appendClassPath(new ByteArrayClassPath(className, clazz));
        CtClass ctClass = pool.get(className);

        // Load world method
        CtClass[] classes = new CtClass[] { pool.get("java.lang.String"), pool.get("java.lang.String"), CtClass.longType,
                pool.get("net.minecraft.server.v1_14_R1.WorldType"), pool.get("com.google.gson.JsonElement") };

        CtMethod loadWorldMethod = ctClass.getDeclaredMethod("a", classes);

        loadWorldMethod.insertBefore("{" +
                "    net.minecraft.server.v1_14_R1.WorldServer[] defaultWorlds = com.grinderwolf.swm.crlfixer.CRLFixer.getDefaultWorlds();" +
                "    " +
                "    if (defaultWorlds != null) {" +
                "        System.out.println(\"Overriding default worlds\");" +
                "        " +
                "        for (int index = 0; index < 3; index++) {" +
                "            net.minecraft.server.v1_14_R1.WorldServer world = defaultWorlds[index];" +
                "            byte dimension = (index == 1 ? -1 : (index == 2 ? 1 : 0));" +
                "            " +
                "            net.minecraft.server.v1_14_R1.WorldSettings worldSettings = new net.minecraft.server.v1_14_R1.WorldSettings($3, $0.getGamemode(), $0.getGenerateStructures(), $0.isHardcore(), $4);" +
                "            java.lang.String worldTypeString = org.bukkit.World.Environment.getEnvironment(dimension).toString().toLowerCase();" +
                "            java.lang.String name;" +
                "            " +
                "            if (dimension == 0) {" +
                "                name = $1;" +
                "            } else {" +
                "                name = $1 + \"_ \" + worldTypeString;" +
                "            }" +
                "            " +
                "            if (world == null) {" +
                "                $0.convertWorld(name);" +
                "                " +
                "                worldSettings.setGeneratorSettings($5);" +
                "            }" +
                "            " +
                "            if (index == 0) {" +
                "                if (world == null) {" +
                "                    net.minecraft.server.v1_14_R1.WorldNBTStorage nbtStorage = new net.minecraft.server.v1_14_R1.WorldNBTStorage(server.getWorldContainer(), $2, $0, $0.dataConverterManager);" +
                "                    net.minecraft.server.v1_14_R1.WorldData worldData = nbtStorage.getWorldData();" +
                "                    " +
                "                    if (worldData == null) {" +
                "                        worldData = new net.minecraft.server.v1_14_R1.WorldData(worldSettings, $2);" +
                "                    }" +
                "                    " +
                "                    worldData.checkName($2);" +
                "                    $0.a(nbtStorage.getDirectory(), worldData);" +
                "                    net.minecraft.server.v1_14_R1.WorldLoadListener worldLoadListener = $0.worldLoadListenerFactory.create(11);" +
                "                    world = new net.minecraft.server.v1_14_R1.WorldServer($0, $0.executorService, nbtStorage, worldData, net.minecraft.server.v1_14_R1.DimensionManager.OVERWORLD, $0.methodProfiler, worldLoadListener, org.bukkit.World.Environment.getEnvironment(dimension), $0.server.getGenerator(name));" +
                "                }" +
                "                " +
                "                $0.initializeScoreboards(world.getWorldPersistentData());" +
                "                $0.server.scoreboardManager = new org.bukkit.craftbukkit.v1_14_R1.scoreboard.CraftScoreboardManager($0, world.getScoreboard());" +
                "            } else if (world == null) {" +
                "                net.minecraft.server.v1_14_R1.WorldNBTStorage nbtStorage = new net.minecraft.server.v1_14_R1.WorldNBTStorage(server.getWorldContainer(), name, $0, $0.dataConverterManager);" +
                "                net.minecraft.server.v1_14_R1.WorldData worldData = nbtStorage.getWorldData();" +
                "                " +
                "                if (worldData == null) {" +
                "                    worldData = new net.minecraft.server.v1_14_R1.WorldData(worldSettings, name);" +
                "                }" +
                "                " +
                "                worldData.checkName(name);" +
                "                net.minecraft.server.v1_14_R1.WorldLoadListener worldLoadListener = $0.worldLoadListenerFactory.create(11);" +
                "                world = new net.minecraft.server.v1_14_R1.SecondaryWorldServer($0.getWorldServer(net.minecraft.server.v1_14_R1.DimensionManager.OVERWORLD), $0, $0.executorService, nbtStorage, net.minecraft.server.v1_14_R1.DimensionManager.a(dimension), $0.methodProfiler, worldLoadListener, worldData, org.bukkit.World.Environment.getEnvironment(dimension), $0.server.getGenerator(name));" +
                "            }" +
                "            " +
                "            $0.initWorld(world, world.getDataManager().getWorldData(), worldSettings);" +
                "            $0.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldInitEvent(world.getWorld()));" +
                "            $0.worldServer.put(world.getWorldProvider().getDimensionManager(), world);" +
                "            $0.getPlayerList().setPlayerFileData(world);" +
                "            " +
                "            if (world.getDataManager().getWorldData().getCustomBossEvents() != null) {" +
                "                $0.getBossBattleCustomData().a(world.getDataManager().getWorldData().getCustomBossEvents());" +
                "            }" +
                "        }" +
                "        " +
                "        $0.a($0.getDifficulty(), true);" +
                "        " +
                "        java.util.Iterator worldList = $0.getWorlds().iterator();" +
                "        " +
                "        while (worldList.hasNext()) {" +
                "            net.minecraft.server.v1_14_R1.WorldServer world = (net.minecraft.server.v1_14_R1.WorldServer) worldList.next();" +
                "            $0.loadSpawn(world.getChunkProvider().playerChunkMap.worldLoadListener, world);" +
                "            $0.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldLoadEvent(world.getWorld()));" +
                "        }" +
                "        " +
                "        $0.server.enablePlugins(org.bukkit.plugin.PluginLoadOrder.POSTWORLD);" +
                "        $0.server.getPluginManager().callEvent(new org.bukkit.event.server.ServerLoadEvent(org.bukkit.event.server.ServerLoadEvent.LoadType.STARTUP));" +
                "        $0.serverConnection.acceptConnections();" +
                "        " +
                "        return;" +
                "    }" +
                "}");

        return ctClass;
    }
}
