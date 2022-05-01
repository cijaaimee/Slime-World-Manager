package com.grinderwolf.swm.nms;

import com.flowpowered.nbt.*;
import com.grinderwolf.swm.api.loaders.*;
import com.grinderwolf.swm.api.world.*;
import com.grinderwolf.swm.api.world.properties.*;
import com.grinderwolf.swm.nms.world.*;
import it.unimi.dsi.fastutil.longs.*;
import org.bukkit.World;

import java.io.IOException;
import java.util.*;

public interface SlimeNMS extends SlimeWorldSource {

    Object injectDefaultWorlds();

    void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld) throws IOException;

    void generateWorld(SlimeWorld world);

    SlimeWorld getSlimeWorld(World world);

    byte getWorldVersion();

}
