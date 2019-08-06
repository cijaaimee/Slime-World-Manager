package com.grinderwolf.swm.nms;

import com.grinderwolf.swm.api.world.SlimeWorld;
import org.bukkit.World;

public interface SlimeNMS {

    public void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld);
    public void generateWorld(SlimeWorld world);
    public SlimeWorld getSlimeWorld(World world);
    public boolean isV1_13WorldFormat();
}
