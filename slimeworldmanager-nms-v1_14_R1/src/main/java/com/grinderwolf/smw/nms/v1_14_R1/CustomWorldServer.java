package com.grinderwolf.smw.nms.v1_14_R1;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.grinderwolf.smw.api.world.SlimeWorld;
import com.grinderwolf.smw.nms.CraftSlimeWorld;
import lombok.Getter;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.ChunkProviderServer;
import net.minecraft.server.v1_14_R1.DimensionManager;
import net.minecraft.server.v1_14_R1.EnumDifficulty;
import net.minecraft.server.v1_14_R1.ExceptionWorldConflict;
import net.minecraft.server.v1_14_R1.IAsyncTaskHandler;
import net.minecraft.server.v1_14_R1.IProgressUpdate;
import net.minecraft.server.v1_14_R1.MinecraftServer;
import net.minecraft.server.v1_14_R1.WorldLoadListener;
import net.minecraft.server.v1_14_R1.WorldNBTStorage;
import net.minecraft.server.v1_14_R1.WorldServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.World;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomWorldServer extends WorldServer {

    private static final Logger LOGGER = LogManager.getLogger("SMW World");
    private static final ExecutorService WORLD_SAVER_SERVICE = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder()
            .setNameFormat("SMW Pool Thread #%1$d").build());

    private static Field playerChunkMapField;
    private static Field serverThreadQueueField;
    private static Field lightEngineField;
    private static Field chunkMapDistanceField;

    static {
        try {
            playerChunkMapField = ChunkProviderServer.class.getDeclaredField("playerChunkMap");
            serverThreadQueueField = ChunkProviderServer.class.getDeclaredField("serverThreadQueue");
            lightEngineField = ChunkProviderServer.class.getDeclaredField("lightEngine");
            chunkMapDistanceField = ChunkProviderServer.class.getDeclaredField("chunkMapDistance");

            playerChunkMapField.setAccessible(true);
            serverThreadQueueField.setAccessible(true);
            lightEngineField.setAccessible(true);
            chunkMapDistanceField.setAccessible(true);
        } catch (NoSuchFieldException ex) {
            ex.printStackTrace();
        }
    }

    @Getter
    private final CraftSlimeWorld slimeWorld;
    private final Object saveLock = new Object();

    public CustomWorldServer(CraftSlimeWorld world, WorldNBTStorage nbtStorage, DimensionManager dimensionManager, WorldLoadListener listener) {
        super(MinecraftServer.getServer(), MinecraftServer.getServer().executorService, nbtStorage, nbtStorage.getWorldData(),
                dimensionManager, MinecraftServer.getServer().getMethodProfiler(), MinecraftServer.getServer().worldLoadListenerFactory.create(11), World.Environment.NORMAL, null);

        this.slimeWorld = world;

        // Have to use reflection to change the PlayerChunkMap sadly :(
        try {
            ChunkProviderServer provider = getChunkProvider();
            CustomChunkMap customChunkMap = new CustomChunkMap(slimeWorld, this, nbtStorage.getDataFixer(), nbtStorage.f(),
                    (IAsyncTaskHandler<Runnable>) serverThreadQueueField.get(provider), provider, provider.getChunkGenerator(),
                    listener, () -> MinecraftServer.getServer().getWorldServer(DimensionManager.OVERWORLD).getWorldPersistentData(), spigotConfig.viewDistance);

            playerChunkMapField.set(provider, customChunkMap);
            lightEngineField.set(provider, customChunkMap.a());
            chunkMapDistanceField.set(provider, customChunkMap.getChunkMapDistance());
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }

        SlimeWorld.SlimeProperties properties = world.getProperties();

        worldData.setDifficulty(EnumDifficulty.getById(properties.getDifficulty()));
        worldData.setSpawn(new BlockPosition(properties.getSpawnX(), properties.getSpawnY(), properties.getSpawnZ()));
        super.setSpawnFlags(properties.allowMonsters(), properties.allowAnimals());
        MinecraftServer.getServer().worldServer.put(dimensionManager, this);
    }

    @Override
    public void save(IProgressUpdate progressUpdate, boolean forceSave, boolean flag1) throws ExceptionWorldConflict {
        if (!slimeWorld.getProperties().isReadOnly()) {
            if (!flag1) { // Don't know what this is at all, but the original method only saves the world when this is false
                org.bukkit.Bukkit.getPluginManager().callEvent(new org.bukkit.event.world.WorldSaveEvent(getWorld())); // CraftBukkit

                if (MinecraftServer.getServer().isStopped()) { // Make sure the slimeWorld gets saved before stopping the server by running it from the main thread
                    save();

                    // Have to manually unlock the world as well
                    try {
                        slimeWorld.getLoader().unlockWorld(slimeWorld.getName());
                    } catch (IOException ex) {
                        LOGGER.error("Failed to unlock the world " + slimeWorld.getName() + ". Please unlock it manually by using the command /smw manualunlock. Stack trace:");

                        ex.printStackTrace();
                    }
                } else {
                    WORLD_SAVER_SERVICE.execute(this::save);
                }
            }
        }
    }

    private void save() {
        synchronized (saveLock) { // Don't want to save the slimeWorld from multiple threads simultaneously
            try {
                LOGGER.info("Saving slimeWorld " + slimeWorld.getName() + "...");
                long start = System.currentTimeMillis();
                byte[] serializedWorld = slimeWorld.serialize();
                slimeWorld.getLoader().saveWorld(slimeWorld.getName(), serializedWorld);
                LOGGER.info("World " + slimeWorld.getName() + " saved in " + (System.currentTimeMillis() - start) + "ms.");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void setSpawnFlags(boolean allowMonsters, boolean allowAnimals) {
        super.setSpawnFlags(allowMonsters, allowAnimals);

        // Keep properties updated
        SlimeWorld.SlimeProperties newProps = slimeWorld.getProperties().toBuilder().allowMonsters(allowMonsters).allowAnimals(allowAnimals).build();
        slimeWorld.setProperties(newProps);
    }
}
