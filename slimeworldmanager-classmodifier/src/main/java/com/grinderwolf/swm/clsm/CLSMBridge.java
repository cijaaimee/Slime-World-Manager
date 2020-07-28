package com.grinderwolf.swm.clsm;

public interface CLSMBridge {

    default Object getChunk(Object world, int x, int z) {
        return null;
    }

    default boolean saveChunk(Object world, Object chunkAccess) {
        return false;
    }

    boolean isCustomWorld(Object world);

    default boolean skipWorldAdd(Object world) {
        return false; // If true, the world won't be added to the bukkit world list
    }
}
