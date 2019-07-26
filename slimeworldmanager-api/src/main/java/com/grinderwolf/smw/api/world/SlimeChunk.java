package com.grinderwolf.smw.api.world;

import com.flowpowered.nbt.CompoundTag;

import java.util.List;

public interface SlimeChunk {

    public int getX();
    public int getZ();
    public SlimeChunkSection[] getSections();
    public int[] getHeightMap();
    public byte[] getBiomes();
    public List<CompoundTag> getTileEntities();
    public List<CompoundTag> getEntities();
}
