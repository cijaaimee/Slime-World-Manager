package com.grinderwolf.swm.nms;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.nms.world.SlimeLoadedWorld;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.List;

public interface SlimeWorldSource {

    SlimeLoadedWorld createSlimeWorld(SlimeLoader loader, String worldName, Long2ObjectOpenHashMap<SlimeChunk> chunks,
                                      CompoundTag extraCompound, List<CompoundTag> mapList, byte worldVersion,
                                      SlimePropertyMap worldPropertyMap, boolean readOnly, boolean lock,
                                      Long2ObjectOpenHashMap<List<CompoundTag>> entities);

}
