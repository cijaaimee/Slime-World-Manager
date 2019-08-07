package com.grinderwolf.swm.clsm;

public interface CLSMBridge {

    public Object getChunk(Object world, int x, int z);
    public boolean saveChunk(Object world, Object chunkAccess);

    // Array containing the normal world, the nether and the end
    public Object[] getDefaultWorlds();
}
