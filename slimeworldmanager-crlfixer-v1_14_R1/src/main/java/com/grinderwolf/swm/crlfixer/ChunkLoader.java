package com.grinderwolf.swm.crlfixer;

import net.minecraft.server.v1_14_R1.Chunk;
import net.minecraft.server.v1_14_R1.IChunkAccess;
import net.minecraft.server.v1_14_R1.WorldServer;

public interface ChunkLoader {

    public Chunk getChunk(WorldServer world, int x, int z);
    public boolean saveChunk(WorldServer world, IChunkAccess chunkAccess);

    // Array containing the normal world, the nether and the end
    public WorldServer[] getDefaultWorlds();
}
