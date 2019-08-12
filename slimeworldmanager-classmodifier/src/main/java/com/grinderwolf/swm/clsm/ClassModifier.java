package com.grinderwolf.swm.clsm;

import com.mojang.datafixers.util.Either;

import java.util.concurrent.CompletableFuture;

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

    private static CLSMBridge customLoader;

    public static CompletableFuture getFutureChunk(Object world, int x, int z) {
        if (customLoader == null) {
            return null;
        }

        Object chunk = customLoader.getChunk(world, x, z);
        return chunk != null ? CompletableFuture.supplyAsync(() -> Either.left(chunk)) : null;
    }

    public static boolean saveChunk(Object world, Object chunkAccess) {
        return customLoader != null && customLoader.saveChunk(world, chunkAccess);
    }

    public static void setLoader(CLSMBridge loader) {
        customLoader = loader;
    }

    public static Object[] getDefaultWorlds() {
        return customLoader != null ? customLoader.getDefaultWorlds() : null;
    }
}
