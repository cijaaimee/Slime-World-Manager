package com.grinderwolf.smw.crlfixer;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.v1_14_R1.Chunk;
import net.minecraft.server.v1_14_R1.IChunkAccess;
import net.minecraft.server.v1_14_R1.ProtoChunkExtension;
import net.minecraft.server.v1_14_R1.WorldServer;

import java.util.concurrent.CompletableFuture;

/**
 * This class serves as a bridge between the SMW and the Minecraft server.
 *
 * As plugins are loaded using a different ClassLoader, their code cannot
 * be accessed from a NMS method. Because of this, it's impossible to make
 * any calls to any method when rewriting the bytecode of the PlayerChunkMap class.
 *
 * As a workaround, this bridge simply calls a method of the {@link ChunkLoader} interface,
 * which is implemented by the SMW plugin when loaded.
 */
public class CRLFixer {

    private static ChunkLoader customLoader;

    public static CompletableFuture getFutureChunk(WorldServer world, int x, int z) {
        if (customLoader == null) {
            return null;
        }

        Chunk chunk = customLoader.getChunk(world, x, z);
        return chunk != null ? CompletableFuture.supplyAsync(() -> Either.left(new ProtoChunkExtension(chunk))) : null;
    }

    public static boolean saveChunk(WorldServer world, IChunkAccess chunkAccess) {
        return customLoader != null && customLoader.saveChunk(world, chunkAccess);
    }

    public static void setLoader(ChunkLoader loader) {
        customLoader = loader;
    }
}
