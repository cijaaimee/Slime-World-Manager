package com.grinderwolf.smw.nms.v1_14_R1;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.LongArrayTag;
import com.grinderwolf.smw.api.world.SlimeChunk;
import com.grinderwolf.smw.api.world.SlimeChunkSection;
import com.grinderwolf.smw.nms.CraftSlimeWorld;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.v1_14_R1.BiomeBase;
import net.minecraft.server.v1_14_R1.Block;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.Chunk;
import net.minecraft.server.v1_14_R1.ChunkConverter;
import net.minecraft.server.v1_14_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_14_R1.ChunkGenerator;
import net.minecraft.server.v1_14_R1.ChunkMapDistance;
import net.minecraft.server.v1_14_R1.ChunkSection;
import net.minecraft.server.v1_14_R1.ChunkStatus;
import net.minecraft.server.v1_14_R1.ChunkTaskQueueSorter;
import net.minecraft.server.v1_14_R1.DefinedStructureManager;
import net.minecraft.server.v1_14_R1.EntityTypes;
import net.minecraft.server.v1_14_R1.EnumSkyBlock;
import net.minecraft.server.v1_14_R1.FluidType;
import net.minecraft.server.v1_14_R1.FluidTypes;
import net.minecraft.server.v1_14_R1.HeightMap;
import net.minecraft.server.v1_14_R1.IAsyncTaskHandler;
import net.minecraft.server.v1_14_R1.IChunkAccess;
import net.minecraft.server.v1_14_R1.ILightAccess;
import net.minecraft.server.v1_14_R1.IRegistry;
import net.minecraft.server.v1_14_R1.LightEngine;
import net.minecraft.server.v1_14_R1.LightEngineThreaded;
import net.minecraft.server.v1_14_R1.MinecraftServer;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.PlayerChunk;
import net.minecraft.server.v1_14_R1.PlayerChunkMap;
import net.minecraft.server.v1_14_R1.ProtoChunkTickList;
import net.minecraft.server.v1_14_R1.SectionPosition;
import net.minecraft.server.v1_14_R1.TileEntity;
import net.minecraft.server.v1_14_R1.WorldLoadListener;
import net.minecraft.server.v1_14_R1.WorldPersistentData;
import net.minecraft.server.v1_14_R1.WorldServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CustomChunkMap extends PlayerChunkMap {

    private static final Logger LOGGER = LogManager.getLogger("SMW Chunk Loader");

    private static Field pendingUnloadField;
    private static Field lightEngineField;
    private static Field sorterField;

    static {
        try {
            pendingUnloadField = PlayerChunkMap.class.getDeclaredField("pendingUnload");
            lightEngineField = PlayerChunkMap.class.getDeclaredField("lightEngine");
            sorterField = PlayerChunkMap.class.getDeclaredField("q");

            pendingUnloadField.setAccessible(true);
            lightEngineField.setAccessible(true);
            sorterField.setAccessible(true);
        } catch (NoSuchFieldException ex) {
            ex.printStackTrace();
        }
    }

    private final CraftSlimeWorld slimeWorld;
    private final CustomChunkMapDistance customChunkMapDistance;
    private Long2ObjectLinkedOpenHashMap<PlayerChunk> pendingUnloadList;
    private LightEngineThreaded lightEngineThreaded;
    private ChunkTaskQueueSorter sorter;

    private boolean updatingChunksModified;

    public CustomChunkMap(CraftSlimeWorld slimeWorld, WorldServer worldserver, DataFixer datafixer, DefinedStructureManager definedstructuremanager, IAsyncTaskHandler<Runnable> iasynctaskhandler, ILightAccess ilightaccess, ChunkGenerator<?> chunkgenerator, WorldLoadListener worldloadlistener, Supplier<WorldPersistentData> supplier, int i) {
        super(worldserver, new File("temp_" + slimeWorld.getName()), datafixer, definedstructuremanager, MinecraftServer.getServer().executorService, iasynctaskhandler, ilightaccess, chunkgenerator, worldloadlistener, supplier, i);

        this.slimeWorld = slimeWorld;
        this.customChunkMapDistance = new CustomChunkMapDistance(MinecraftServer.getServer().executorService, iasynctaskhandler);

        try {
            pendingUnloadList = (Long2ObjectLinkedOpenHashMap<PlayerChunk>) pendingUnloadField.get(this);
            lightEngineThreaded = (LightEngineThreaded) lightEngineField.get(this);
            sorter = (ChunkTaskQueueSorter) sorterField.get(this);
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> a(PlayerChunk playerChunk, ChunkStatus chunkStatus) {
        return CompletableFuture.supplyAsync(() -> {

            Chunk chunk = loadChunk(playerChunk.i());
            return Either.left(chunk);

        });
    }

    @Override
    protected boolean b() {
        if (!this.updatingChunksModified) {
            return false;
        } else {
            this.visibleChunks = this.updatingChunks.clone();
            this.updatingChunksModified = false;
            return true;
        }
    }

    private Chunk loadChunk(ChunkCoordIntPair pos) {
        LOGGER.debug("Loading chunk (" + pos.x + ", " + pos.z + ") on world " + slimeWorld.getName());

        SlimeChunk chunk = slimeWorld.getChunk(pos.x, pos.z);
        BlockPosition.MutableBlockPosition mutableBlockPosition = new BlockPosition.MutableBlockPosition();

        // ProtoChunkTickLists
        ProtoChunkTickList<Block> airChunkTickList = new ProtoChunkTickList<>((block) -> block == null || block.getBlockData().isAir(), pos);
        ProtoChunkTickList<FluidType> fluidChunkTickList = new ProtoChunkTickList<>((fluidType) -> fluidType == FluidTypes.EMPTY, pos);

        if (chunk == null) {
            long index = (((long) pos.z) * Integer.MAX_VALUE + ((long) pos.x));

            LOGGER.debug("Failed to load chunk (" + pos.x + ", " + pos.z + ") (" + index + ") on world " + slimeWorld.getName() + ": chunk does not exist. Generating empty one...");

            BiomeBase[] biomeBaseArray = new BiomeBase[256];

            for (int i = 0; i < biomeBaseArray.length; i++) {
                biomeBaseArray[i] = world.getChunkProvider().getChunkGenerator().getWorldChunkManager().getBiome(mutableBlockPosition
                        .d((i & 15) + (pos.x << 4), 0, (i >> 4 & 15) + (pos.z << 4)));
            }

            Chunk nmsChunk = new Chunk(world, pos, biomeBaseArray, ChunkConverter.a, airChunkTickList, fluidChunkTickList, 0L, new ChunkSection[16], null);
            HeightMap.a(nmsChunk, nmsChunk.getChunkStatus().h());

            return nmsChunk;
        }

        // Biomes
        BiomeBase[] biomeBaseArray = new BiomeBase[256];
        int[] biomeIntArray = chunk.getBiomes();

        for (int i = 0; i < biomeIntArray.length; i++) {
            biomeBaseArray[i] = IRegistry.BIOME.fromId(biomeIntArray[i]);

            if (biomeBaseArray[i] == null) {
                biomeBaseArray[i] = world.getChunkProvider().getChunkGenerator().getWorldChunkManager().getBiome(mutableBlockPosition
                        .c((i & 15) + (pos.x << 4), 0, (i >> 4 & 15) + (pos.z << 4)));
            }
        }

        // chunk sections
        LOGGER.debug("Loading chunk sections for chunk (" + pos.x + ", " + pos.z + ") on world " + slimeWorld.getName());
        ChunkSection[] sections = new ChunkSection[16];
        LightEngine lightEngine = world.getChunkProvider().getLightEngine();

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            SlimeChunkSection slimeSection = chunk.getSections()[sectionId];

            if (slimeSection != null) {
                ChunkSection section = new ChunkSection(sectionId);

                LOGGER.debug("ChunkSection #" + sectionId + " - Chunk (" + pos.x + ", " + pos.z + ") - World " + slimeWorld.getName() + ":");
                LOGGER.debug("Block palette:");
                LOGGER.debug(slimeSection.getPalette().toString());
                LOGGER.debug("Block states array:");
                LOGGER.debug(slimeSection.getBlockStates());
                LOGGER.debug("Block light array:");
                LOGGER.debug(slimeSection.getBlockLight().getBacking());
                LOGGER.debug("Sky light array:");
                LOGGER.debug(slimeSection.getSkyLight().getBacking());

                NBTTagCompound sectionCompound = new NBTTagCompound();

                sectionCompound.set("Palette", Converter.convertTag(slimeSection.getPalette()));
                sectionCompound.a("BlockStates", slimeSection.getBlockStates());

                section.getBlocks().a(sectionCompound, "Palette", "BlockStates");
                lightEngine.a(EnumSkyBlock.BLOCK, SectionPosition.a(pos, sectionId), Converter.convertArray(slimeSection.getBlockLight()));
                lightEngine.a(EnumSkyBlock.SKY, SectionPosition.a(pos, sectionId), Converter.convertArray(slimeSection.getSkyLight()));

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
                    TileEntity entity = TileEntity.create((NBTTagCompound) Converter.convertTag(tag));

                    if (entity != null) {
                        nmsChunk.a(entity);
                        loadedEntities++;
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

        Chunk nmsChunk = new Chunk(world, pos, biomeBaseArray, ChunkConverter.a, airChunkTickList, fluidChunkTickList, 0L, sections, loadEntities);

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

    @Override
    public boolean saveChunk(IChunkAccess chunkAccess) {
        if (chunkAccess.isNeedsSaving()) {
            chunkAccess.setNeedsSaving(false);
        }

        ChunkStatus status = chunkAccess.getChunkStatus();

        if (status.getType() == ChunkStatus.Type.LEVELCHUNK) {
            SlimeChunk slimeChunk = Converter.convertChunk((Chunk) chunkAccess);
            this.slimeWorld.updateChunk(slimeChunk);
        }

        return true;
    }

    // The original method is protected, not public
    public LightEngineThreaded a() {
        return super.a();
    }

    // This replaces the original e() method. Not only the method is protected, but also the object it returns, so it cannot be overridden
    public CustomChunkMapDistance getChunkMapDistance() {
        return customChunkMapDistance;
    }

    class CustomChunkMapDistance extends ChunkMapDistance {

        private CustomChunkMapDistance(Executor executor, Executor executor1) {
            super(executor, executor1);
        }

        protected boolean a(long i) {
            return unloadQueue.contains(i);
        }

        @Nullable
        protected PlayerChunk b(long i) {
            return getUpdatingChunk(i);
        }

        @Nullable
        protected PlayerChunk a(long i, int j, @Nullable PlayerChunk playerchunk, int k) {
            if (k > GOLDEN_TICKET && j > GOLDEN_TICKET) {
                return playerchunk;
            } else {
                if (playerchunk != null) {
                    playerchunk.a(j);
                }

                if (playerchunk != null) {
                    if (j > GOLDEN_TICKET) {
                        unloadQueue.add(i);
                    } else {
                        unloadQueue.remove(i);
                    }
                }

                if (j <= GOLDEN_TICKET && playerchunk == null) {
                    playerchunk = (PlayerChunk) pendingUnloadList.remove(i);
                    if (playerchunk != null) {
                        playerchunk.a(j);
                    } else {
                        playerchunk = new PlayerChunk(new ChunkCoordIntPair(i), j, lightEngineThreaded, sorter, CustomChunkMap.this);
                    }

                    CustomChunkMap.this.updatingChunks.put(i, playerchunk);
                    CustomChunkMap.this.updatingChunksModified = true;
                }

                return playerchunk;
            }
        }
    }
}
