package com.grinderwolf.swm.clsm;

public interface CLSMBridge {

    public default Object getChunk(Object world, int x, int z) {
        return null;
    }

    public default boolean saveChunk(Object world, Object chunkAccess) {
        return false;
    }

    // Array containing the normal world, the nether and the end
    public Object[] getDefaultWorlds();
}
