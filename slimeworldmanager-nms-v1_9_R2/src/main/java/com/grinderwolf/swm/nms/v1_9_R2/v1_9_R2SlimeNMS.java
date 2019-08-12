package com.grinderwolf.swm.nms.v1_9_R2;

import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.grinderwolf.swm.nms.SlimeNMS;
import lombok.Getter;
import net.minecraft.server.v1_9_R2.MinecraftServer;
import net.minecraft.server.v1_9_R2.WorldServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_9_R2.CraftWorld;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;

@Getter
public class v1_9_R2SlimeNMS implements SlimeNMS {

    private static final Logger LOGGER = LogManager.getLogger("SWM");

    private final boolean v1_13WorldFormat = false;
    private WorldServer defaultWorld;
    private WorldServer defaultNetherWorld;
    private WorldServer defaultEndWorld;

    public v1_9_R2SlimeNMS() {
        try {
            CraftCLSMBridge.initialize(this);
        }  catch (NoClassDefFoundError ex) {
            LOGGER.warn("Failed to find ClassModifier classes. Overriding default worlds is disabled.");
        }
    }

    @Override
    public void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld) {
        if (normalWorld != null) {
            defaultWorld = new CustomWorldServer((CraftSlimeWorld) normalWorld, new CustomDataManager(normalWorld), 0);
        }

        if (netherWorld != null) {
            defaultNetherWorld = new CustomWorldServer((CraftSlimeWorld) netherWorld, new CustomDataManager(netherWorld), -1);
        }

        if (netherWorld != null) {
            defaultEndWorld = new CustomWorldServer((CraftSlimeWorld) endWorld, new CustomDataManager(endWorld), 1);
        }
    }

    @Override
    public void generateWorld(SlimeWorld world) {
        String worldName = world.getName();

        if (Bukkit.getWorld(worldName) != null) {
            throw new IllegalArgumentException("World " + worldName + " already exists! Maybe it's an outdated SlimeWorld object?");
        }

        LOGGER.info("Loading world " + world.getName());
        long startTime = System.currentTimeMillis();

        CustomDataManager dataManager = new CustomDataManager(world);
        MinecraftServer mcServer = MinecraftServer.getServer();
        int dimension = CraftWorld.CUSTOM_DIMENSION_OFFSET + mcServer.worlds.size();

        for (WorldServer server : mcServer.worlds) {
            if (server.dimension == dimension) {
                dimension++;
            }
        }

        WorldServer server = new CustomWorldServer((CraftSlimeWorld) world, dataManager, dimension);

        mcServer.worlds.add(server);

        Bukkit.getPluginManager().callEvent(new WorldInitEvent(server.getWorld()));
        Bukkit.getPluginManager().callEvent(new WorldLoadEvent(server.getWorld()));

        if (server.getWorld().getKeepSpawnInMemory()) {
            LOGGER.debug("Preparing start region for world " + worldName);
            long timeMillis = System.currentTimeMillis();

            for (int x = -196; x <= 196; x += 16) {
                for (int z = -196; z <= 196; z += 16) {
                    long currentTime = System.currentTimeMillis();

                    if (currentTime > timeMillis + 1000L) {
                        int total = (196 * 2 + 1) * (196 * 2 + 1);
                        int done = (x + 196) * (196 * 2 + 1) + z + 1;

                        LOGGER.debug("Preparing spawn area for " + worldName + ": " + (done * 100 / total) + "%");
                        timeMillis = currentTime;
                    }
                }
            }
        }

        LOGGER.info("World " + world.getName() + " loaded in " + (System.currentTimeMillis() - startTime) + "ms.");
    }

    @Override
    public SlimeWorld getSlimeWorld(World world) {
        CraftWorld craftWorld = (CraftWorld) world;

        if (!(craftWorld.getHandle() instanceof CustomWorldServer)) {
            return null;
        }

        CustomWorldServer worldServer = (CustomWorldServer) craftWorld.getHandle();

        return worldServer.getSlimeWorld();
    }
}
