package com.grinderwolf.smw.nms.v1_8_R3;

import com.grinderwolf.smw.api.SlimeWorld;
import com.grinderwolf.smw.nms.SlimeNMS;
import net.minecraft.server.v1_8_R3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class v1_8_R3SlimeNMS implements SlimeNMS {

    private static final Logger LOGGER = LogManager.getLogger("SMW NMS implementation");

    @Override
    public void inject() {
        // Nothing to do really
    }

    @Override
    public void loadWorld(SlimeWorld world) {
        String worldName = world.getName();

        if (Bukkit.getWorld(worldName) != null) {
            throw new IllegalArgumentException("World " + worldName + " already exists!");
        }

        LOGGER.info("Trying to load world " + world.getName());
        long startTime = System.currentTimeMillis();

        CustomDataManager dataManager = new CustomDataManager(world);
        MinecraftServer mcServer = MinecraftServer.getServer();
        int dimension = CraftWorld.CUSTOM_DIMENSION_OFFSET + mcServer.worlds.size();

        for (WorldServer server : mcServer.worlds) {
            if (server.dimension == dimension) {
                dimension++;
            }
        }

        WorldServer server = new WorldServer(mcServer, dataManager, dataManager.getWorldData(), dimension, mcServer.methodProfiler, World.Environment.NORMAL, null);

        server.b();
        server.scoreboard = mcServer.server.getScoreboardManager().getMainScoreboard().getHandle();
        server.tracker = new EntityTracker(server);
        server.addIWorldAccess(new WorldManager(mcServer, server));
        server.worldData.setDifficulty(EnumDifficulty.PEACEFUL);
        server.setSpawnFlags(true, true); // allowMonsters and allowAnimals
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
    public void unloadWorld(SlimeWorld world) {

    }
}
