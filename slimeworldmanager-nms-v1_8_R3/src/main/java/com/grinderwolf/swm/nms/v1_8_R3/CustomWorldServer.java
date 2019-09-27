package com.grinderwolf.swm.nms.v1_8_R3;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Chunk;
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

    private static final Logger LOGGER = LogManager.getLogger("SWM World");
    private static final ExecutorService WORLD_SAVER_SERVICE = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder()
            .setNameFormat("SWM Pool Thread #%1$d").build());

    @Getter
    private final CraftSlimeWorld slimeWorld;
    private final Object saveLock = new Object();

    @Getter
    @Setter
    private boolean ready = false;

    CustomWorldServer(CraftSlimeWorld world, IDataManager dataManager, int dimension) {
        super(MinecraftServer.getServer(), dataManager, dataManager.getWorldData(), dimension, MinecraftServer.getServer().methodProfiler,
                World.Environment.valueOf(world.getProperties().getEnvironment()), null);

        b();
        this.slimeWorld = world;
        this.tracker = new EntityTracker(this);
        addIWorldAccess(new WorldManager(MinecraftServer.getServer(), this));

        // Set world properties
        SlimeWorld.SlimeProperties properties = world.getProperties();

        worldData.setDifficulty(EnumDifficulty.getById(properties.getDifficulty()));
        worldData.setSpawn(new BlockPosition(properties.getSpawnX(), properties.getSpawnY(), properties.getSpawnZ()));
        super.setSpawnFlags(properties.allowMonsters(), properties.allowAnimals());

        this.pvpMode = properties.isPvp();

        // Load all chunks
        CustomChunkLoader chunkLoader = ((CustomDataManager) this.getDataManager()).getChunkLoader();
        chunkLoader.loadAllChunks(this);
    }

    @Override
    public void save(boolean forceSave, IProgressUpdate progressUpdate) throws ExceptionWorldConflict {
        if (!slimeWorld.getProperties().isReadOnly()) {
            super.save(forceSave, progressUpdate);

            if (MinecraftServer.getServer().isStopped()) { // Make sure the SlimeWorld gets saved before stopping the server by running it from the main thread
                save();

                // Have to manually unlock the world as well
                try {
                    slimeWorld.getLoader().unlockWorld(slimeWorld.getName());
                } catch (IOException ex) {
                    LOGGER.error("Failed to unlock the world " + slimeWorld.getName() + ". Please unlock it manually by using the command /swm manualunlock. Stack trace:");

                    ex.printStackTrace();
                } catch (UnknownWorldException ignored) {

                }
            } else {
                WORLD_SAVER_SERVICE.execute(this::save);
            }
        }
    }

    private void save() {
        synchronized (saveLock) { // Don't want to save the slimeWorld from multiple threads simultaneously
            try {
                LOGGER.info("Saving world " + slimeWorld.getName() + "...");
                long start = System.currentTimeMillis();

                CustomChunkLoader chunkLoader = ((CustomDataManager) this.getDataManager()).getChunkLoader();

                for (Chunk nmsChunk : chunkLoader.getChunks()) {
                    SlimeChunk chunk = Converter.convertChunk(nmsChunk);
                    slimeWorld.updateChunk(chunk);
                }

                byte[] serializedWorld = slimeWorld.serialize();
                slimeWorld.getLoader().saveWorld(slimeWorld.getName(), serializedWorld, false);

                slimeWorld.clearChunks();
                LOGGER.info("World " + slimeWorld.getName() + " saved in " + (System.currentTimeMillis() - start) + "ms.");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
