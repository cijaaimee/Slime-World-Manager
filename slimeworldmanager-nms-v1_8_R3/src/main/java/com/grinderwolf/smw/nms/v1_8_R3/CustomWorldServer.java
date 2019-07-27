package com.grinderwolf.smw.nms.v1_8_R3;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.grinderwolf.smw.api.world.SlimeWorld;
import com.grinderwolf.smw.nms.CraftSlimeWorld;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityTracker;
import net.minecraft.server.v1_8_R3.EnumDifficulty;
import net.minecraft.server.v1_8_R3.ExceptionWorldConflict;
import net.minecraft.server.v1_8_R3.IDataManager;
import net.minecraft.server.v1_8_R3.IProgressUpdate;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.WorldManager;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.World;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomWorldServer extends WorldServer {

    private static final Logger LOGGER = LogManager.getLogger("SMW World");
    private static final ExecutorService WORLD_SAVER_SERVICE = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder()
            .setNameFormat("SMW Pool Thread #%1$d").build());
    private final CraftSlimeWorld world;

    private final Object saveLock = new Object();

    public CustomWorldServer(CraftSlimeWorld world, IDataManager dataManager, int dimension) {
        super(MinecraftServer.getServer(), dataManager, dataManager.getWorldData(), dimension, MinecraftServer.getServer().methodProfiler, World.Environment.NORMAL, null);

        b();
        this.world = world;
        this.scoreboard = MinecraftServer.getServer().server.getScoreboardManager().getMainScoreboard().getHandle();
        this.tracker = new EntityTracker(this);
        addIWorldAccess(new WorldManager(MinecraftServer.getServer(), this));

        SlimeWorld.SlimeProperties properties = world.getProperties();

        worldData.setDifficulty(EnumDifficulty.getById(properties.getDifficulty()));
        worldData.setSpawn(new BlockPosition(properties.getSpawnX(), properties.getSpawnY(), properties.getSpawnZ()));
        setSpawnFlags(properties.allowMonsters(), properties.allowAnimals());
    }

    @Override
    public void save(boolean forceSave, IProgressUpdate progressUpdate) throws ExceptionWorldConflict {
        super.save(forceSave, progressUpdate);

        if (forceSave) { // Make sure the world gets saved before stopping the server by running it from the main thread
            save();
        } else {
            WORLD_SAVER_SERVICE.execute(this::save);
        }
    }

    private void save() {
        synchronized (saveLock) { // Don't want to save the world from multiple threads simultaneously
            try {
                LOGGER.info("Saving world " + world.getName() + "...");
                long start = System.currentTimeMillis();
                byte[] serializedWorld = world.serialize();
                world.getLoader().saveWorld(world.getName(), serializedWorld);
                LOGGER.info("World " + world.getName() + " saved in " + (System.currentTimeMillis() - start) + "ms.");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
