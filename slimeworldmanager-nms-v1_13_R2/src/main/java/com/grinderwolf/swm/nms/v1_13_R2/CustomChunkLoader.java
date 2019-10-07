package com.grinderwolf.swm.nms.v1_13_R2;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.LongArrayTag;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.nms.CraftSlimeChunk;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_13_R2.BiomeBase;
import net.minecraft.server.v1_13_R2.Biomes;
import net.minecraft.server.v1_13_R2.Block;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.Chunk;
import net.minecraft.server.v1_13_R2.ChunkConverter;
import net.minecraft.server.v1_13_R2.ChunkCoordIntPair;
import net.minecraft.server.v1_13_R2.ChunkSection;
import net.minecraft.server.v1_13_R2.ChunkStatus;
import net.minecraft.server.v1_13_R2.DimensionManager;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.EntityTypes;
import net.minecraft.server.v1_13_R2.FluidType;
import net.minecraft.server.v1_13_R2.FluidTypes;
import net.minecraft.server.v1_13_R2.GeneratorAccess;
import net.minecraft.server.v1_13_R2.HeightMap;
import net.minecraft.server.v1_13_R2.IChunkAccess;
import net.minecraft.server.v1_13_R2.IChunkLoader;
import net.minecraft.server.v1_13_R2.IRegistry;
import net.minecraft.server.v1_13_R2.NBTBase;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.NBTTagList;
import net.minecraft.server.v1_13_R2.PersistentCollection;
import net.minecraft.server.v1_13_R2.ProtoChunk;
import net.minecraft.server.v1_13_R2.ProtoChunkTickList;
import net.minecraft.server.v1_13_R2.TileEntity;
import net.minecraft.server.v1_13_R2.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class CustomChunkLoader implements IChunkLoader {

    private static final Logger LOGGER = LogManager.getLogger("SWM Chunk Loader");

    private final NBTTagCompound emptyWorldCompound = new NBTTagCompound();
    private final CraftSlimeWorld world;

    {
        emptyWorldCompound.set("Level", new NBTTagCompound());
    }

    void loadAllChunks(CustomWorldServer server) {
        for (SlimeChunk chunk : new ArrayList<>(world.getChunks().values())) {
            Object[] data = createChunk(server, chunk);
            world.updateChunk(new NMSSlimeChunk((Chunk) data[0], (NBTTagCompound) data[1]));
        }
    }

    private Object[] createChunk(CustomWorldServer server, SlimeChunk chunk) {
        int x = chunk.getX();
        int z = chunk.getZ();

        LOGGER.debug("Loading chunk (" + x + ", " + z + ") on world " + world.getName());
        Object[] data = new Object[2];

        // Fake level compound
        NBTTagCompound levelCompound = new NBTTagCompound();
        NBTTagCompound globalCompound = new NBTTagCompound();

        globalCompound.set("Level", levelCompound);
        data[1] = globalCompound;

        BlockPosition.MutableBlockPosition mutableBlockPosition = new BlockPosition.MutableBlockPosition();

        // ProtoChunkTickLists
        ProtoChunkTickList<Block> airChunkTickList = new ProtoChunkTickList<>((block) -> block.getBlockData().isAir(), IRegistry.BLOCK::getKey, IRegistry.BLOCK::getOrDefault, new ChunkCoordIntPair(x, z));
        ProtoChunkTickList<FluidType> fluidChunkTickList = new ProtoChunkTickList<>((fluidType) -> fluidType == FluidTypes.EMPTY, IRegistry.FLUID::getKey, IRegistry.FLUID::getOrDefault, new ChunkCoordIntPair(x, z));

        // Biomes
        BiomeBase[] biomeBaseArray = new BiomeBase[256];
        int[] biomeIntArray = chunk.getBiomes();

        for (int i = 0; i < biomeIntArray.length; i++) {
            biomeBaseArray[i] = IRegistry.BIOME.fromId(biomeIntArray[i]);

            if (biomeBaseArray[i] == null) {
                biomeBaseArray[i] = server.getChunkProvider().getChunkGenerator().getWorldChunkManager().getBiome(mutableBlockPosition
                        .c((i & 15) + (x << 4), 0, (i >> 4 & 15) + (z << 4)), Biomes.PLAINS);
            }
        }

        CompoundTag upgradeDataTag = ((CraftSlimeChunk) chunk).getUpgradeData();
        Chunk nmsChunk = new Chunk(server.getMinecraftWorld(), x, z, biomeBaseArray, upgradeDataTag == null ? ChunkConverter.a
                : new ChunkConverter((NBTTagCompound) Converter.convertTag(upgradeDataTag)), airChunkTickList, fluidChunkTickList, 0L);

        // Chunk status
        nmsChunk.c("postprocessed");

        // Chunk sections
        LOGGER.debug("Loading chunk sections for chunk (" + x + ", " + z + ") on world " + world.getName());
        ChunkSection[] sections = new ChunkSection[16];

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            SlimeChunkSection slimeSection = chunk.getSections()[sectionId];

            if (slimeSection != null && slimeSection.getBlockStates().length > 0) { // If block states array is empty, it's just a fake chunk made by the createEmptyWorld method
                ChunkSection section = new ChunkSection(sectionId << 4, true);

                LOGGER.debug("ChunkSection #" + sectionId + " - Chunk (" + x + ", " + z + ") - World " + world.getName() + ":");
                LOGGER.debug("Block palette:");
                LOGGER.debug(slimeSection.getPalette().toString());
                LOGGER.debug("Block states array:");
                LOGGER.debug(slimeSection.getBlockStates());
                LOGGER.debug("Block light array:");
                LOGGER.debug(slimeSection.getBlockLight() != null ? slimeSection.getBlockLight().getBacking() : "Not present");
                LOGGER.debug("Sky light array:");
                LOGGER.debug(slimeSection.getSkyLight() != null ? slimeSection.getSkyLight().getBacking() : "Not present");

                NBTTagCompound sectionCompound = new NBTTagCompound();

                sectionCompound.set("Palette", Converter.convertTag(slimeSection.getPalette()));
                sectionCompound.a("BlockStates", slimeSection.getBlockStates());

                section.getBlocks().a(sectionCompound, "Palette", "BlockStates");

                if (slimeSection.getBlockLight() != null) {
                    section.a(Converter.convertArray(slimeSection.getBlockLight()));
                }

                if (slimeSection.getSkyLight() != null) {
                    section.b(Converter.convertArray(slimeSection.getSkyLight()));
                }

                section.recalcBlockCounts();
                sections[sectionId] = section;
            }
        }

        nmsChunk.a(sections);

        // Height Maps
        HeightMap.Type[] heightMapTypes = HeightMap.Type.values();
        CompoundMap heightMaps = chunk.getHeightMaps().getValue();

        for (HeightMap.Type type : heightMapTypes) {
            if (type.c() == HeightMap.Use.LIVE_WORLD) {
                String name = type.b();

                if (heightMaps.containsKey(name)) {
                    LongArrayTag heightMap = (LongArrayTag) heightMaps.get(name);

                    nmsChunk.a(type, heightMap.getValue());
                } else {
                    nmsChunk.b(type).a();
                }
            }
        }

        // Tile Entities
        if (chunk.getTileEntities() != null) {
            NBTTagList tileEntities = new NBTTagList();

            for (CompoundTag tileEntityTag : chunk.getTileEntities()) {
                tileEntities.add(Converter.convertTag(tileEntityTag));
            }

            levelCompound.set("TileEntities", tileEntities);
        }

        // Entities
        if (chunk.getEntities() != null) {
            NBTTagList entities = new NBTTagList();

            for (CompoundTag entityTag : chunk.getEntities()) {
                entities.add(Converter.convertTag(entityTag));
            }

            levelCompound.set("Entities", entities);
        }

        LOGGER.debug("Loaded chunk (" + x + ", " + z + ") on world " + world.getName());
        data[0] = nmsChunk;

        return data;
    }

    @Nullable
    @Override
    public ProtoChunk b(GeneratorAccess generatorAccess, int x, int z, Consumer<IChunkAccess> consumer) {
        Chunk chunk = this.a(generatorAccess, x, z, null);
        consumer.accept(chunk);

        return null;
    }

    // Load full chunk
    @Override
    public Chunk a(GeneratorAccess generatorAccess, int x, int z, Consumer<Chunk> consumer) {
        Object[] chunkData = loadChunk(generatorAccess, x, z, consumer);
        Chunk chunk = (Chunk) chunkData[0];

        // Chunk consumer
        NBTTagCompound compound = (NBTTagCompound) chunkData[1];

        if (consumer != null) {
            consumer.accept(chunk);
        }

        loadEntities(compound.getCompound("Level"), chunk);
        return chunk;
    }

    // PaperSpigot adds these methods to the IChunkLoader interface, so they have to be implemented as well

    public Object[] loadChunk(GeneratorAccess generatorAccess, int x, int z, Consumer<Chunk> consumer) { // Paper method
        SlimeChunk slimeChunk = world.getChunk(x, z);
        Object[] data;

        if (slimeChunk == null) {
            data = new Object[2];
            BlockPosition.MutableBlockPosition mutableBlockPosition = new BlockPosition.MutableBlockPosition();

            // Biomes
            BiomeBase[] biomeBaseArray = new BiomeBase[256];

            for (int i = 0; i < biomeBaseArray.length; i++) {
                biomeBaseArray[i] = generatorAccess.getChunkProvider().getChunkGenerator().getWorldChunkManager().getBiome(mutableBlockPosition
                        .c((i & 15) + (x << 4), 0, (i >> 4 & 15) + (z << 4)), Biomes.PLAINS);
            }

            // ProtoChunkTickLists
            ProtoChunkTickList<Block> airChunkTickList = new ProtoChunkTickList<>((block) -> block.getBlockData().isAir(), IRegistry.BLOCK::getKey, IRegistry.BLOCK::getOrDefault, new ChunkCoordIntPair(x, z));
            ProtoChunkTickList<FluidType> fluidChunkTickList = new ProtoChunkTickList<>((fluidType) -> fluidType == FluidTypes.EMPTY, IRegistry.FLUID::getKey, IRegistry.FLUID::getOrDefault, new ChunkCoordIntPair(x, z));

            Chunk nmsChunk = new Chunk(generatorAccess.getMinecraftWorld(), x, z, biomeBaseArray, ChunkConverter.a, airChunkTickList, fluidChunkTickList, 0L);
            nmsChunk.c("postprocessed");

            data[0] = nmsChunk;
            data[1] = emptyWorldCompound;
        } else if (slimeChunk instanceof NMSSlimeChunk) {
            NMSSlimeChunk nmsSlimeChunk = (NMSSlimeChunk) slimeChunk;
            data = new Object[] { nmsSlimeChunk.getChunk(), nmsSlimeChunk.getCompound() };
        } else { // All SlimeChunk objects should be converted to NMSSlimeChunks when loading the world
            throw new IllegalStateException("Chunk (" + x + ", " + z + ") has not been converted to a NMSSlimeChunk object!");
        }

        return data;
    }

    public void getPersistentStructureLegacy(DimensionManager manager, PersistentCollection collection) { } // Paper method

    public void loadEntities(NBTTagCompound compound, Chunk chunk) { // Paper method
        // Load tile entities
        LOGGER.debug("Loading tile entities for chunk (" + chunk.locX + ", " + chunk.locZ + ") on world " + world.getName());
        int loadedEntities = 0;

        if (compound.hasKeyOfType("TileEntities", 9)) {
            NBTTagList tileEntities = compound.getList("TileEntities", 10);

            for (NBTBase tileEntity : tileEntities) {
                TileEntity entity = TileEntity.create((NBTTagCompound) tileEntity);

                if (entity != null) {
                    chunk.a(entity);
                    loadedEntities++;
                }
            }
        }

        LOGGER.debug("Loaded " + loadedEntities + " tile entities for chunk (" + chunk.locX + ", " + chunk.locZ + ") on world " + world.getName());

        // Load entities
        LOGGER.debug("Loading entities for chunk (" + chunk.locX + ", " + chunk.locZ + ") on world " + world.getName());
        loadedEntities = 0;

        if (compound.hasKeyOfType("Entities", 9)) {
            NBTTagList entities = compound.getList("Entities", 10);

            for (NBTBase entity : entities) {
                loadEntity((NBTTagCompound) entity, chunk);
            }
        }

        LOGGER.debug("Loaded " + loadedEntities + " entities for chunk (" + chunk.locX + ", " + chunk.locZ + ") on world " + world.getName());
    }

    private Entity loadEntity(NBTTagCompound tag, Chunk chunk) {
        Entity entity = EntityTypes.a(tag, chunk.getWorld());
        chunk.f(true);

        if (entity != null) {
            chunk.a(entity);

            if (tag.hasKeyOfType("Passengers", 9)) {
                NBTTagList passengersList = tag.getList("Passengers", 10);

                for (NBTBase passengerTag : passengersList) {
                    Entity passenger = loadEntity((NBTTagCompound) passengerTag, chunk);

                    if (passengerTag != null) {
                        passenger.a(entity, true);
                    }
                }
            }
        }

        return entity;
    }

    @Override
    public void saveChunk(World world, IChunkAccess chunkAccess) {
        this.saveChunk(world, chunkAccess, false);
    }

    @Override
    public void saveChunk(World world, IChunkAccess chunkAccess, boolean unloaded) {
        if (chunkAccess.i().d() == ChunkStatus.Type.PROTOCHUNK) {
            return; // Not saving protochunks, only full chunks
        }

        SlimeChunk slimeChunk = this.world.getChunk(chunkAccess.getPos().x, chunkAccess.getPos().z);

        if (slimeChunk instanceof NMSSlimeChunk) { // In case somehow the chunk object changes (might happen for some reason)
            ((NMSSlimeChunk) slimeChunk).setChunk((Chunk) chunkAccess);
        } else {
            this.world.updateChunk(new NMSSlimeChunk((Chunk) chunkAccess, emptyWorldCompound));
        }
    }

    // Save all chunks
    @Override
    public void b() {

    }
}
