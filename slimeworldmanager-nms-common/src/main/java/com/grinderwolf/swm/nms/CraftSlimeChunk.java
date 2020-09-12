package com.grinderwolf.swm.nms;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class CraftSlimeChunk implements SlimeChunk {

    private final String worldName;
    private final int x;
    private final int z;

    @Setter
    private final SlimeChunkSection[] sections;
    private final CompoundTag heightMaps;
    private final int[] biomes;
    private final List<CompoundTag> tileEntities;
    private final List<CompoundTag> entities;

    // Optional data for 1.13 world upgrading
    private CompoundTag upgradeData;

    CraftSlimeChunk(String worldName, SlimeChunk base) {
        this.worldName = worldName;
        this.x = base.getX();
        this.z = base.getZ();

        this.sections = base.getSections();
        this.heightMaps = base.getHeightMaps();
        this.biomes = base.getBiomes();
        this.tileEntities = base.getTileEntities();
        this.entities = base.getEntities();

        if (base instanceof CraftSlimeChunk) {
            this.upgradeData = ((CraftSlimeChunk) base).getUpgradeData();
        } else {
            this.upgradeData = null;
        }
    }

}
