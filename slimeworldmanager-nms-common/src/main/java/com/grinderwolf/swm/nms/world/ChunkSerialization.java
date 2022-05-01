package com.grinderwolf.swm.nms.world;

import com.flowpowered.nbt.*;

import java.util.*;

public record ChunkSerialization(byte[] chunks, List<CompoundTag> tileEntities, List<CompoundTag> entities) {
}
