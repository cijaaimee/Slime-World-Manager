package com.grinderwolf.swm.nms.v1_16_R1;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.LongArrayTag;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.nms.CraftSlimeChunk;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import net.minecraft.server.v1_16_R1.BiomeStorage;
import net.minecraft.server.v1_16_R1.Block;
import net.minecraft.server.v1_16_R1.BlockPosition;
import net.minecraft.server.v1_16_R1.Chunk;
import net.minecraft.server.v1_16_R1.ChunkConverter;
import net.minecraft.server.v1_16_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R1.ChunkSection;
import net.minecraft.server.v1_16_R1.Convertable;
import net.minecraft.server.v1_16_R1.DimensionManager;
import net.minecraft.server.v1_16_R1.EntityTypes;
import net.minecraft.server.v1_16_R1.EnumDifficulty;
import net.minecraft.server.v1_16_R1.EnumSkyBlock;
import net.minecraft.server.v1_16_R1.FluidType;
import net.minecraft.server.v1_16_R1.HeightMap;
import net.minecraft.server.v1_16_R1.IProgressUpdate;
import net.minecraft.server.v1_16_R1.IRegistry;
import net.minecraft.server.v1_16_R1.LightEngine;
import net.minecraft.server.v1_16_R1.MinecraftServer;
import net.minecraft.server.v1_16_R1.MobSpawner;
import net.minecraft.server.v1_16_R1.NBTTagCompound;
import net.minecraft.server.v1_16_R1.NBTTagList;
import net.minecraft.server.v1_16_R1.ProtoChunkExtension;
import net.minecraft.server.v1_16_R1.ResourceKey;
import net.minecraft.server.v1_16_R1.SectionPosition;
import net.minecraft.server.v1_16_R1.TickListChunk;
import net.minecraft.server.v1_16_R1.TileEntity;
import net.minecraft.server.v1_16_R1.WorldDataServer;
import net.minecraft.server.v1_16_R1.WorldMap;
import net.minecraft.server.v1_16_R1.WorldNBTStorage;
import net.minecraft.server.v1_16_R1.WorldServer;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R1.CraftServer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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
    private final List<WorldMap> maps = new ArrayList<>();
    private final CustomNBTStorage nbtStorage;

    @Getter
    @Setter
    private boolean ready = false;

    CustomWorldServer(CraftSlimeWorld world, CustomNBTStorage nbtStorage, Convertable.ConversionSession conversionSession, DimensionManager dimensionManager, World.Environment env, WorldDataServer worldDataServer, ResourceKey<net.minecraft.server.v1_16_R1.World> resourceKey, ResourceKey<DimensionManager> resourceKey1, List<MobSpawner> list, boolean flag, boolean flag1) throws IOException {
        super(
            ((CraftServer)Bukkit.getServer()).getServer(),
            ((CraftServer)Bukkit.getServer()).getServer().executorService,
            conversionSession,
            worldDataServer,
            resourceKey,
            resourceKey1,
            dimensionManager,
            ((CraftServer)Bukkit.getServer()).getServer().worldLoadListenerFactory.create(11),
            worldDataServer.getGeneratorSettings().getChunkGenerator(),
            flag,
            11,
            list,
            flag1,
            env,
            ((CraftServer)Bukkit.getServer()).getServer().D().generator
        );

        // MinecraftServer.getServer().getMethodProfiler()

        this.slimeWorld = world;

        this.nbtStorage = nbtStorage;

        SlimePropertyMap propertyMap = world.getPropertyMap();

        worldDataServer.setDifficulty(EnumDifficulty.valueOf(propertyMap.getString(SlimeProperties.DIFFICULTY).toUpperCase()));
        worldDataServer.setSpawn(new BlockPosition(propertyMap.getInt(SlimeProperties.SPAWN_X), propertyMap.getInt(SlimeProperties.SPAWN_Y), propertyMap.getInt(SlimeProperties.SPAWN_Z)));
        super.setSpawnFlags(propertyMap.getBoolean(SlimeProperties.ALLOW_MONSTERS), propertyMap.getBoolean(SlimeProperties.ALLOW_ANIMALS));

        this.pvpMode = propertyMap.getBoolean(SlimeProperties.PVP);

        new File(nbtStorage.getPlayerDir(), "session.lock").delete();
        new File(nbtStorage.getPlayerDir(), "data").delete();

        nbtStorage.getPlayerDir().delete();
        nbtStorage.getPlayerDir().getParentFile().delete();

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

            nbtStorage.saveWorldData(worldData);

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
            ChunkCoordIntPair pos = new ChunkCoordIntPair(x, z);

            // Biomes
            BiomeStorage biomeStorage = new BiomeStorage(pos, getChunkProvider().getChunkGenerator().getWorldChunkManager(), null);

            // Tick lists
            TickListChunk<Block> airChunkTickList = new TickListChunk<>(IRegistry.BLOCK::getKey, new ArrayList<>(), 0);
            TickListChunk<FluidType> fluidChunkTickList = new TickListChunk<>(IRegistry.FLUID::getKey, new ArrayList<>(), 0);
//            TickListChunk<Block> airChunkTickList = new TickListChunk(IRegistry.BLOCK.getKey(null), new ArrayList<Object>(), (long)0);
//            TickListChunk<FluidType> fluidChunkTickList = new TickListChunk(IRegistry.FLUID::getKey, new ArrayList<>());

            chunk = new Chunk(this, pos, biomeStorage, ChunkConverter.a, airChunkTickList, fluidChunkTickList, 0L, null, null);
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

        // Biomes
        int[] biomeIntArray = chunk.getBiomes();
        BiomeStorage biomeStorage = new BiomeStorage(pos, getChunkProvider().getChunkGenerator().getWorldChunkManager(), biomeIntArray);

        // Tick lists
        TickListChunk<Block> airChunkTickList = new TickListChunk<>(IRegistry.BLOCK::getKey, new ArrayList<>(), 0);
        TickListChunk<FluidType> fluidChunkTickList = new TickListChunk<>(IRegistry.FLUID::getKey, new ArrayList<>(), 0);

        // Chunk sections
        LOGGER.debug("Loading chunk sections for chunk (" + pos.x + ", " + pos.z + ") on world " + slimeWorld.getName());
        ChunkSection[] sections = new ChunkSection[16];
        LightEngine lightEngine = getChunkProvider().getLightEngine();

        lightEngine.b(pos, true);

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            SlimeChunkSection slimeSection = chunk.getSections()[sectionId];

            if (slimeSection != null) {
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
                    lightEngine.a();
                    lightEngine.a(EnumSkyBlock.BLOCK, SectionPosition.a(pos, sectionId), Converter.convertArray(slimeSection.getBlockLight()), true);
                }

                if (slimeSection.getSkyLight() != null) {
                    lightEngine.a(EnumSkyBlock.SKY, SectionPosition.a(pos, sectionId), Converter.convertArray(slimeSection.getSkyLight()), true);
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
                        TileEntity entity = TileEntity.create(null, (NBTTagCompound) Converter.convertTag(tag));

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
        Chunk nmsChunk = new Chunk(this, pos, biomeStorage, upgradeDataTag == null ? ChunkConverter.a : new ChunkConverter((NBTTagCompound)
                Converter.convertTag(upgradeDataTag)), airChunkTickList, fluidChunkTickList, 0L, sections, loadEntities);

        // Height Maps
        EnumSet<HeightMap.Type> heightMapTypes = nmsChunk.getChunkStatus().h();
        CompoundMap heightMaps = chunk.getHeightMaps().getValue();
        EnumSet<HeightMap.Type> unsetHeightMaps = EnumSet.noneOf(HeightMap.Type.class);

        for (HeightMap.Type type : heightMapTypes) {
            String name = type.b();

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