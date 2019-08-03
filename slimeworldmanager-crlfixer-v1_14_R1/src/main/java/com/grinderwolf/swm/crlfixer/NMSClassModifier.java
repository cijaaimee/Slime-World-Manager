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
                "    net.minecraft.server.v1_14_R1.WorldServer slimeWorld = com.grinderwolf.swm.crlfixer.CRLFixer.getDefaultWorld();" +
                "    " +
                "    if (slimeWorld != null) {" +
                "        System.out.println(\"Overriding default world\");" +
                "        $0.initializeScoreboards(slimeWorld.getWorldPersistentData());" +
                "        $0.server.scoreboardManager = new org.bukkit.craftbukkit.v1_14_R1.scoreboard.CraftScoreboardManager($0, slimeWorld.getScoreboard());" +
                "        $0.initWorld(slimeWorld, slimeWorld.getDataManager().getWorldData(), new net.minecraft.server.v1_14_R1.WorldSettings($3, $0.getGamemode(), $0.getGenerateStructures(), $0.isHardcore(), $4));" +
                "        $0.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldInitEvent(slimeWorld.getWorld()));" +
                "        $0.worldServer.put(slimeWorld.getWorldProvider().getDimensionManager(), slimeWorld);" +
                "        $0.getPlayerList().setPlayerFileData(slimeWorld);" +
                "        $0.a($0.getDifficulty(), true);" +
                "        $0.loadSpawn(slimeWorld.getChunkProvider().playerChunkMap.worldLoadListener, slimeWorld);" +
                "        $0.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldLoadEvent(slimeWorld.getWorld()));" +
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
