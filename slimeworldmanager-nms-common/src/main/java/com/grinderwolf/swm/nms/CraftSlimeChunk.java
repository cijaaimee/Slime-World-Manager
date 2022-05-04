package com.grinderwolf.swm.nms;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CraftSlimeChunk implements SlimeChunk {

    private final String worldName;
    private final int x;
    private final int z;

    @Setter
    private SlimeChunkSection[] sections;
    private final CompoundTag heightMaps;
    private final int[] biomes;
    private final List<CompoundTag> tileEntities;
    private final List<CompoundTag> entities;

    @Setter
    private int minSection;
    @Setter
    private int maxSection;

}
