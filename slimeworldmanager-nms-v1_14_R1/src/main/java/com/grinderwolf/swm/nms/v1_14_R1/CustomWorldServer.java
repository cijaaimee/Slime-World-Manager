package com.grinderwolf.swm.nms.v1_14_R1;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.LongArrayTag;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.nms.CraftSlimeChunk;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.v1_14_R1.BiomeBase;
import net.minecraft.server.v1_14_R1.Block;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.Chunk;
import net.minecraft.server.v1_14_R1.ChunkConverter;
import net.minecraft.server.v1_14_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_14_R1.ChunkSection;
import net.minecraft.server.v1_14_R1.DimensionManager;
import net.minecraft.server.v1_14_R1.EntityTypes;
import net.minecraft.server.v1_14_R1.EnumDifficulty;
import net.minecraft.server.v1_14_R1.EnumSkyBlock;
import net.minecraft.server.v1_14_R1.FluidType;
import net.minecraft.server.v1_14_R1.HeightMap;
import net.minecraft.server.v1_14_R1.IProgressUpdate;
import net.minecraft.server.v1_14_R1.IRegistry;
import net.minecraft.server.v1_14_R1.LightEngine;
import net.minecraft.server.v1_14_R1.MinecraftServer;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.NBTTagList;
import net.minecraft.server.v1_14_R1.ProtoChunkExtension;
import net.minecraft.server.v1_14_R1.SectionPosition;
import net.minecraft.server.v1_14_R1.TickListChunk;
import net.minecraft.server.v1_14_R1.TileEntity;
import net.minecraft.server.v1_14_R1.WorldNBTStorage;
import net.minecraft.server.v1_14_R1.WorldServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class CustomWorldServer extends WorldServer {

    private static final Logger LOGGER = LogManager.getLogger("SWM World");
    private static final ExecutorService WORLD_SAVER_SERVICE = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder()
            .setNameFormat("SWM Pool Thread #%1$d").build());

    @Getter
    private final CraftSlimeWorld slimeWorld;
    private final Object saveLock = new Object();

    private final Map<Long, Chunk> chunks = new HashMap<>();

    @Getter
    @Setter
    private boolean ready = false;

    CustomWorldServer(CraftSlimeWorld world, WorldNBTStorage nbtStorage, DimensionManager dimensionManager) {
        super(MinecraftServer.getServer(), MinecraftServer.getServer().executorService, nbtStorage, nbtStorage.getWorldData(),
                dimensionManager, MinecraftServer.getServer().getMethodProfiler(), MinecraftServer.getServer().worldLoadListenerFactory.create(11), World.Environment.NORMAL, null);

        this.slimeWorld = world;

        SlimeWorld.SlimeProperties properties = world.getProperties();

        worldData.setDifficulty(EnumDifficulty.getById(properties.getDifficulty()));
        worldData.setSpawn(new BlockPosition(properties.getSpawnX(), properties.getSpawnY(), properties.getSpawnZ()));
        worldData.d(true);
        super.setSpawnFlags(properties.allowMonsters(), properties.allowAnimals());

        new File(nbtStorage.getDirectory(), "session.lock").delete();
        new File(nbtStorage.getDirectory(), "data").delete();

        nbtStorage.getDirectory().delete();
        nbtStorage.getDirectory().getParentFile().delete();

        this.pvpMode = properties.isPvp();

        loadAllChunks();
    }

    @Override
    public void save(IProgressUpdate progressUpdate, boolean forceSave, boolean flag1) {
        if (!slimeWorld.getProperties().isReadOnly()) {
            org.bukkit.Bukkit.getPluginManager().callEvent(new org.bukkit.event.world.WorldSaveEvent(getWorld())); // CraftBukkit
            this.getChunkProvider().save(forceSave);

            if (MinecraftServer.getServer().isStopped()) { // Make sure the slimeWorld gets saved before stopping the server by running it from the main thread
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

                synchronized (chunks) {
                    for (Chunk nmsChunk : chunks.values()) {
                        SlimeChunk chunk = Converter.convertChunk(nmsChunk);
                        slimeWorld.updateChunk(chunk);
                    }
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

    @Override
    public void setSpawnFlags(boolean allowMonsters, boolean allowAnimals) {
        super.setSpawnFlags(allowMonsters, allowAnimals);

        // Keep properties updated
        SlimeWorld.SlimeProperties newProps = slimeWorld.getProperties().toBuilder().allowMonsters(allowMonsters).allowAnimals(allowAnimals).build();
        slimeWorld.setProperties(newProps);
    }

    private void loadAllChunks() {
        for (SlimeChunk chunk : slimeWorld.getChunks().values()) {
            Chunk nmsChunk = createChunk(chunk);

            long index = (((long) chunk.getZ()) * Integer.MAX_VALUE + ((long) chunk.getX()));
            chunks.put(index, nmsChunk);
        }

        // Now that we've converted every SlimeChunk to its nms counterpart, we can empty the chunk list
        slimeWorld.clearChunks();
    }

    private Chunk createChunk(SlimeChunk chunk) {
        int x = chunk.getX();
        int z = chunk.getZ();

        LOGGER.debug("Loading chunk (" + x + ", " + z + ") on world " + slimeWorld.getName());

        ChunkCoordIntPair pos = new ChunkCoordIntPair(x, z);
        BlockPosition.MutableBlockPosition mutableBlockPosition = new BlockPosition.MutableBlockPosition();

        // Tick lists
        TickListChunk<Block> airChunkTickList = new TickListChunk<>(IRegistry.BLOCK::getKey, new ArrayList<>());
        TickListChunk<FluidType> fluidChunkTickList = new TickListChunk<>(IRegistry.FLUID::getKey, new ArrayList<>());

        // Biomes
        BiomeBase[] biomeBaseArray = new BiomeBase[256];
        int[] biomeIntArray = chunk.getBiomes();

        for (int i = 0; i < biomeIntArray.length; i++) {
            biomeBaseArray[i] = IRegistry.BIOME.fromId(biomeIntArray[i]);

            if (biomeBaseArray[i] == null) {
                biomeBaseArray[i] = getChunkProvider().getChunkGenerator().getWorldChunkManager().getBiome(mutableBlockPosition
                        .c((i & 15) + (pos.x << 4), 0, (i >> 4 & 15) + (pos.z << 4)));
            }
        }

        // Chunk sections
        LOGGER.debug("Loading chunk sections for chunk (" + pos.x + ", " + pos.z + ") on world " + slimeWorld.getName());
        ChunkSection[] sections = new ChunkSection[16];
        LightEngine lightEngine = getChunkProvider().getLightEngine();

        lightEngine.b(pos, true);

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            SlimeChunkSection slimeSection = chunk.getSections()[sectionId];

            if (slimeSection != null && slimeSection.getBlockStates().length > 0) { // If block states array is empty, it's just a fake chunk made by the createEmptyWorld method
                ChunkSection section = new ChunkSection(sectionId << 4);

                LOGGER.debug("ChunkSection #" + sectionId + " - Chunk (" + pos.x + ", " + pos.z + ") - World " + slimeWorld.getName() + ":");
                LOGGER.debug("Block palette:");
                LOGGER.debug(slimeSection.getPalette().toString());
                LOGGER.debug("Block states array:");
                LOGGER.debug(slimeSection.getBlockStates());
                LOGGER.debug("Block light array:");
                LOGGER.debug(slimeSection.getBlockLight() != null ? slimeSection.getBlockLight().getBacking() : "Not present");
                LOGGER.debug("Sky light array:");
                LOGGER.debug(slimeSection.getSkyLight() != null ? slimeSection.getSkyLight().getBacking() : "Not present");

                section.getBlocks().a((NBTTagList) Converter.convertTag(slimeSection.getPalette()), slimeSection.getBlockStates());

                if (slimeSection.getBlockLight() != null) {
                    lightEngine.a(EnumSkyBlock.BLOCK, SectionPosition.a(pos, sectionId), Converter.convertArray(slimeSection.getBlockLight()));
                }

                if (slimeSection.getSkyLight() != null) {
                    lightEngine.a(EnumSkyBlock.SKY, SectionPosition.a(pos, sectionId), Converter.convertArray(slimeSection.getSkyLight()));
                }

                section.recalcBlockCounts();
                sections[sectionId] = section;
            }
        }

        Consumer<Chunk> loadEntities = (nmsChunk) -> {

            // Load tile entities
            LOGGER.debug("Loading tile entities for chunk (" + pos.x + ", " + pos.z + ") on world " + slimeWorld.getName());
            List<CompoundTag> tileEntities = chunk.getTileEntities();
            int loadedEntities = 0;

            if (tileEntities != null) {
                for (CompoundTag tag : tileEntities) {
                    Optional<String> type = tag.getStringValue("id");

                    // Sometimes null tile entities are saved
                    if (type.isPresent()) {
                        TileEntity entity = TileEntity.create((NBTTagCompound) Converter.convertTag(tag));

                        if (entity != null) {
                            nmsChunk.a(entity);
                            loadedEntities++;
                        }
                    }
                }
            }

            LOGGER.debug("Loaded " + loadedEntities + " tile entities for chunk (" + pos.x + ", " + pos.z + ") on world " + slimeWorld.getName());

            // Load entities
            LOGGER.debug("Loading entities for chunk (" + pos.x + ", " + pos.z + ") on world " + slimeWorld.getName());
            List<CompoundTag> entities = chunk.getEntities();
            loadedEntities = 0;

            if (entities != null) {
                for (CompoundTag tag : entities) {
                    EntityTypes.a((NBTTagCompound) Converter.convertTag(tag), nmsChunk.world, (entity) -> {

                        nmsChunk.a(entity);
                        return entity;

                    });

                    nmsChunk.d(true);
                    loadedEntities++;
                }
            }

            LOGGER.debug("Loaded " + loadedEntities + " entities for chunk (" + pos.x + ", " + pos.z + ") on world " + slimeWorld.getName());
            LOGGER.debug("Loaded chunk (" + pos.x + ", " + pos.z + ") on world " + slimeWorld.getName());

        };

        CompoundTag upgradeDataTag = ((CraftSlimeChunk) chunk).getUpgradeData();
        Chunk nmsChunk = new Chunk(this, pos, biomeBaseArray, upgradeDataTag == null ? ChunkConverter.a : new ChunkConverter((NBTTagCompound)
                Converter.convertTag(upgradeDataTag)), airChunkTickList, fluidChunkTickList, 0L, sections, loadEntities);

        // Height Maps
        EnumSet<HeightMap.Type> heightMapTypes = nmsChunk.getChunkStatus().h();
        CompoundMap heightMaps = chunk.getHeightMaps().getValue();
        EnumSet<HeightMap.Type> unsetHeightMaps = EnumSet.noneOf(HeightMap.Type.class);

        for (HeightMap.Type type : heightMapTypes) {
            String name = type.a();

            if (heightMaps.containsKey(name)) {
                LongArrayTag heightMap = (LongArrayTag) heightMaps.get(name);
                nmsChunk.a(type, heightMap.getValue());
            } else {
                unsetHeightMaps.add(type);
            }
        }

        HeightMap.a(nmsChunk, unsetHeightMaps);

        return nmsChunk;
    }

    ProtoChunkExtension getChunk(int x, int z) {
        long index = (((long) z) * Integer.MAX_VALUE + ((long) x));
        Chunk chunk;

        synchronized (chunks) {
            chunk = chunks.get(index);
        }

        if (chunk == null) {
            BiomeBase[] biomeBaseArray = new BiomeBase[256];

            BlockPosition.MutableBlockPosition mutableBlockPosition = new BlockPosition.MutableBlockPosition();

            for (int i = 0; i < biomeBaseArray.length; i++) {
                biomeBaseArray[i] = getChunkProvider().getChunkGenerator().getWorldChunkManager().getBiome(mutableBlockPosition
                        .d((i & 15) + (x << 4), 0, (i >> 4 & 15) + (z << 4)));
            }

            // Tick lists
            TickListChunk<Block> airChunkTickList = new TickListChunk<>(IRegistry.BLOCK::getKey, new ArrayList<>());
            TickListChunk<FluidType> fluidChunkTickList = new TickListChunk<>(IRegistry.FLUID::getKey, new ArrayList<>());

            ChunkCoordIntPair pos = new ChunkCoordIntPair(x, z);
            chunk = new Chunk(this, pos, biomeBaseArray, ChunkConverter.a, airChunkTickList, fluidChunkTickList, 0L, null, null);
            HeightMap.a(chunk, chunk.getChunkStatus().h());

            getChunkProvider().getLightEngine().b(pos, true);
        }

        return new ProtoChunkExtension(chunk);
    }

    void saveChunk(Chunk chunk) {
        boolean empty = true;

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            ChunkSection section = chunk.getSections()[sectionId];

            if (section != null) {
                section.recalcBlockCounts();

                if (!section.c()) {
                    empty = false;
                    break;
                }
            }
        }

        long index = (((long) chunk.getPos().z) * Integer.MAX_VALUE + ((long) chunk.getPos().x));

        synchronized (chunks) {
            if (empty) {
                chunks.remove(index);
            } else {
                chunks.putIfAbsent(index, chunk);
            }
        }
    }
}