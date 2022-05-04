package com.grinderwolf.swm.nms;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ListTag;
import com.grinderwolf.swm.api.utils.NibbleArray;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Getter
@AllArgsConstructor
public class CraftSlimeChunkSection implements SlimeChunkSection {

    // Post 1.13 block data
    private final ListTag<CompoundTag> palette;
    private final long[] blockStates;

    // Post 1.17 block data
    @Setter
    private CompoundTag blockStatesTag;
    @Setter
    private CompoundTag biomeTag;

    @Nullable
    private final NibbleArray blockLight;
    @Nullable
    private final NibbleArray skyLight;

    @Override
    public byte[] getBlocks() {
        return null;
    }

    @Override
    public NibbleArray getData() {
        return null;
    }
}
