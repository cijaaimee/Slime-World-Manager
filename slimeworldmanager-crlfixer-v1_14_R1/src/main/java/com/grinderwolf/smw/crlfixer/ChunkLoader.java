package com.grinderwolf.smw.crlfixer;

import net.minecraft.server.v1_14_R1.Chunk;
import net.minecraft.server.v1_14_R1.WorldServer;

public interface ChunkLoader {

    public Chunk getChunk(WorldServer world, int x, int z);
}
