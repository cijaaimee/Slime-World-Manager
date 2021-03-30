package com.grinderwolf.swm.clsm;

import com.mojang.datafixers.util.Either;
import org.bukkit.Bukkit;

import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

/**
 * This class serves as a bridge between the SWM and the Minecraft server.
 *
 * As plugins are loaded using a different ClassLoader, their code cannot
 * be accessed from a NMS method. Because of this, it's impossible to make
 * any calls to any method when rewriting the bytecode of a NMS class.
 *
 * As a workaround, this bridge simply calls a method of the {@link CLSMBridge} interface,
 * which is implemented by the SWM plugin when loaded.
 */
public class ClassModifier {

    // Required for Paper 1.13 as javassist can't compile this class
    public static final BooleanSupplier BOOLEAN_SUPPLIER = () -> true;

    private static CLSMBridge customLoader;

//    private void d () {
//        try {
//            Class.forName("").isInstance(this);
//        } catch(ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//    }

    public static CompletableFuture getFutureChunk(Object world, int x, int z) {
        if (customLoader == null || !isCustomWorld(world)) {
            return null;
        }

        return CompletableFuture.supplyAsync(() ->
            Either.left(customLoader.getChunk(world, x, z))
        );
    }

    public static boolean saveChunk(Object world, Object chunkAccess) {
        return customLoader != null && customLoader.saveChunk(world, chunkAccess);
    }

    public static boolean isCustomWorld(Object world) {
        return customLoader != null && customLoader.isCustomWorld(world);
    }

    public static boolean skipWorldAdd(Object world) {
        return customLoader != null && customLoader.skipWorldAdd(world);
    }

    public static void setLoader(CLSMBridge loader) {
        customLoader = loader;
    }

    public static Object[] getDefaultWorlds() {
        return customLoader != null ? customLoader.getDefaultWorlds() : null;
    }

    public static Object getDefaultGamemode() {
        return customLoader != null ? customLoader.getDefaultGamemode() : null;
    }
}
