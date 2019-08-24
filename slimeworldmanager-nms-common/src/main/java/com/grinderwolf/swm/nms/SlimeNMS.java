package com.grinderwolf.swm.nms;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.world.SlimeWorld;
import org.bukkit.World;

public interface SlimeNMS {

    void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld);
    void generateWorld(SlimeWorld world);
    SlimeWorld getSlimeWorld(World world);
    byte getWorldVersion();

    default CompoundTag convertChunk(CompoundTag chunkTag) {
        return chunkTag;
    }
}
