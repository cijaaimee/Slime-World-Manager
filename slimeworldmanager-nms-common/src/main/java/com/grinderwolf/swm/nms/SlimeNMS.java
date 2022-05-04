package com.grinderwolf.swm.nms;

import com.grinderwolf.swm.api.world.SlimeWorld;
import org.bukkit.World;

import java.io.IOException;

public interface SlimeNMS extends SlimeWorldSource {

    Object injectDefaultWorlds();

    void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld) throws IOException;

    void generateWorld(SlimeWorld world);

    SlimeWorld getSlimeWorld(World world);

    byte getWorldVersion();

}
