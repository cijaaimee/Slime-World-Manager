package com.grinderwolf.smw.nms.v1_8_R3;

import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.IPlayerFileData;
import net.minecraft.server.v1_8_R3.NBTTagCompound;

public class EmptyPlayerFileData implements IPlayerFileData {

    @Override public void save(EntityHuman entityHuman) { }
    @Override public NBTTagCompound load(EntityHuman entityHuman) {
        return null;
    }
    @Override public String[] getSeenPlayers() {
        return new String[0];
    }
}
