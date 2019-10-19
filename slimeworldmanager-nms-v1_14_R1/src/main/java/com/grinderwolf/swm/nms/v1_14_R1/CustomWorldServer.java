package com.grinderwolf.swm.nms.v1_14_R1;

import com.flowpowered.nbt.*;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.nms.CraftSlimeChunk;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.v1_14_R1.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.util.*;
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
    private final List<WorldMap> maps = new ArrayList<>();

    @Getter
    @Setter
    private boolean ready = false;

    CustomWorldServer(CraftSlimeWorld world, WorldNBTStorage nbtStorage, DimensionManager dimensionManager, World.Environment env) {
        super(MinecraftServer.getServer(), MinecraftServer.getServer().executorService, nbtStorage, nbtStorage.getWorldData(),
                dimensionManager, MinecraftServer.getServer().getMethodProfiler(), MinecraftServer.getServer().worldLoadListenerFactory.create(11), env, null);

        this.slimeWorld = world;

        SlimePropertyMap propertyMap = world.getPropertyMap();

        worldData.setDifficulty(EnumDifficulty.valueOf(propertyMap.getString(SlimeProperties.DIFFICULTY).toUpperCase()));
        worldData.setSpawn(new BlockPosition(propertyMap.getInt(SlimeProperties.SPAWN_X), propertyMap.getInt(SlimeProperties.SPAWN_Y), propertyMap.getInt(SlimeProperties.SPAWN_Z)));
        super.setSpawnFlags(propertyMap.getBoolean(SlimeProperties.ALLOW_MONSTERS), propertyMap.getBoolean(SlimeProperties.ALLOW_ANIMALS));

        this.pvpMode = propertyMap.getBoolean(SlimeProperties.PVP);

        new File(nbtStorage.getDirectory(), "session.lock").delete();
        new File(nbtStorage.getDirectory(), "data").delete();

        nbtStorage.getDirectory().delete();
        nbtStorage.getDirectory().getParentFile().delete();

        for (CompoundTag mapTag : world.getWorldMaps()) {
            int id = mapTag.getIntValue("id").get();
            WorldMap map = new WorldMap("map_" + id);
            map.a((NBTTagCompound) Converter.convertTag(mapTag));
            a(map);
        }
    }

    @Override
    public void save(IProgressUpdate progressUpdate, boolean forceSave, boolean flag1) {
        if (!slimeWorld.isReadOnly()) {
            org.bukkit.Bukkit.getPluginManager().callEvent(new org.bukkit.event.world.WorldSaveEvent(getWorld())); // CraftBukkit
            this.getChunkProvider().save(forceSave);
            this.getDataManager().saveWorldData(worldData, null);

            // Update the map compound list
            slimeWorld.getWorldMaps().clear();

            for (WorldMap map : maps) {
                NBTTagCompound compound = map.b(new NBTTagCompound());
                int id = Integer.parseInt(map.getId().substring(4));
                compound.setInt("id", id);

                slimeWorld.getWorldMaps().add((CompoundTag) Converter.convertTag("", compound));
            }

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
        synchronized (saveLock) { // Don't want to save the SlimeWorld from multiple threads simultaneously
            try {
                LOGGER.info("Saving world " + slimeWorld.getName() + "...");
                long start = System.currentTimeMillis();
                byte[] serializedWorld = slimeWorld.serialize();
                slimeWorld.getLoader().saveWorld(slimeWorld.getName(), serializedWorld, false);
                LOGGER.info("World " + slimeWorld.getName() + " saved in " + (System.currentTimeMillis() - start) + "ms.");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    ProtoChunkExtension getChunk(int x, int z) {
        SlimeChunk slimeChunk = slimeWorld.getChunk(x, z);
        Chunk chunk;

        if (slimeChunk == null) {
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
        } else if (slimeChunk instanceof NMSSlimeChunk) {
            chunk = ((NMSSlimeChunk) slimeChunk).getChunk();
        } else {
            chunk = createChunk(slimeChunk);
            slimeWorld.updateChunk(new NMSSlimeChunk(chunk));
        }

        return new ProtoChunkExtension(chunk);
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
        LOGGER.debug("Loaded chunk (" + pos.x + ", " + pos.z + ") on world " + slimeWorld.getName());

        return nmsChunk;
    }

    void saveChunk(Chunk chunk) {
        SlimeChunk slimeChunk = slimeWorld.getChunk(chunk.getPos().x, chunk.getPos().z);

        if (slimeChunk instanceof NMSSlimeChunk) { // In case somehow the chunk object changes (might happen for some reason)
            ((NMSSlimeChunk) slimeChunk).setChunk(chunk);
        } else {
            slimeWorld.updateChunk(new NMSSlimeChunk(chunk));
        }
    }

    @Override
    public WorldMap a(String name) {
        int id = Integer.parseInt(name.substring(4));

        if (id >= maps.size()) {
            return null;//super.a(name);
        }

        return maps.get(id);
    }

    @Override
    public void a(WorldMap map) {
        int id = Integer.parseInt(map.getId().substring(4));

        if (maps.size() > id) {
            maps.set(id, map);
        } else {
            while (maps.size() < id) {
                maps.add(null);
            }

            maps.add(id, map);
        }
    }

    @Override
    public int getWorldMapCount() {
        return maps.size();
    }
}