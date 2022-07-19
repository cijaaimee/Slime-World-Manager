/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.nms;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.world.SlimeWorld;
import org.bukkit.World;

public interface SlimeNMS {

    void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld);

    void generateWorld(SlimeWorld world);

    default Object createNMSWorld(SlimeWorld world) {
        throw new UnsupportedOperationException(
                "This spigot version does not support async world loading");
    }

    default void addWorldToServerList(Object worldObject) {
        throw new UnsupportedOperationException(
                "This spigot version does not support async world loading");
    }

    SlimeWorld getSlimeWorld(World world);

    byte getWorldVersion();

    default CompoundTag convertChunk(CompoundTag chunkTag) {
        return chunkTag;
    }
}
