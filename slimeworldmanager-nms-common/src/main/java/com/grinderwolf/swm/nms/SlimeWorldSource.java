package com.grinderwolf.swm.nms;

import com.flowpowered.nbt.*;
import com.grinderwolf.swm.api.loaders.*;
import com.grinderwolf.swm.api.world.*;
import com.grinderwolf.swm.api.world.properties.*;
import com.grinderwolf.swm.nms.world.*;
import it.unimi.dsi.fastutil.longs.*;

import java.util.*;

public interface SlimeWorldSource {

    SlimeLoadedWorld createSlimeWorld(SlimeLoader loader, String worldName, Long2ObjectOpenHashMap<SlimeChunk> chunks,
                                      CompoundTag extraCompound, List<CompoundTag> mapList, byte worldVersion,
                                      SlimePropertyMap worldPropertyMap, boolean readOnly, boolean lock,
                                      Long2ObjectOpenHashMap<List<CompoundTag>> entities);

}
