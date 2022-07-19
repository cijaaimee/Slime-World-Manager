/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.clsm;

public interface CLSMBridge {

    default Object getChunk(Object world, int x, int z) {
        return null;
    }

    default boolean saveChunk(Object world, Object chunkAccess) {
        return false;
    }

    // Array containing the normal world, the nether and the end
    Object[] getDefaultWorlds();

    boolean isCustomWorld(Object world);

    default boolean skipWorldAdd(Object world) {
        return false; // If true, the world won't be added to the bukkit world list
    }
}
