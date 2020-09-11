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

    // When creating a world in 1.16, the WorldServer constructor sets the world's gamemode
    // to the value that the server has as the default gamemode. However, when overriding
    // the default world, this value is not yet accessible (savedata in Minecraftserver is
    // null at this stage), so this method acts as a patch to avoid that NPE in the constructor
    default Object getDefaultGamemode() {
        return null;
    }

}
