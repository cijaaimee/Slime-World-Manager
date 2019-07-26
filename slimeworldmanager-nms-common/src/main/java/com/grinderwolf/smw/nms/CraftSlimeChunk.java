package com.grinderwolf.smw.nms;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ListTag;
import com.grinderwolf.smw.api.SlimeChunk;
import com.grinderwolf.smw.api.SlimeChunkSection;
import com.grinderwolf.smw.api.SlimeWorld;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class CraftSlimeChunk implements SlimeChunk {

    private final String worldName;
    private final int x;
    private final int z;

    private final SlimeChunkSection[] sections;
    private final int[] heightMap;
    private final byte[] biomes;
    private final List<CompoundTag> tileEntities;
    private final List<CompoundTag> entities;
}
