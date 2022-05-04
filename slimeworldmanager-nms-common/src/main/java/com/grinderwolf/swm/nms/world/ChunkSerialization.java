package com.grinderwolf.swm.nms.world;

import com.flowpowered.nbt.CompoundTag;

import java.util.List;

public record ChunkSerialization(byte[] chunks, List<CompoundTag> tileEntities, List<CompoundTag> entities) {
}
