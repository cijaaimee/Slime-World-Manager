package com.grinderwolf.smw.api.world;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ListTag;
import com.grinderwolf.smw.api.utils.NibbleArray;

public interface SlimeChunkSection {

    // Pre 1.13 block format
    public byte[] getBlocks();

    // Post 1.13 block format
    public ListTag<CompoundTag> getPalette();
    public long[] getBlockStates();

    public NibbleArray getData();
    public NibbleArray getBlockLight();
    public NibbleArray getSkyLight();
}
