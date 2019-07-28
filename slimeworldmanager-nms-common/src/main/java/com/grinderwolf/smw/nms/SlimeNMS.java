package com.grinderwolf.smw.nms;

import com.grinderwolf.smw.api.world.SlimeWorld;
import org.bukkit.World;

public interface SlimeNMS {

    public void generateWorld(SlimeWorld world);
    public SlimeWorld getSlimeWorld(World world);
}
