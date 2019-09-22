package com.grinderwolf.swm.nms.v1_13_R1;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.LongArrayTag;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.nms.CraftSlimeChunk;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_13_R1.BiomeBase;
import net.minecraft.server.v1_13_R1.Biomes;
import net.minecraft.server.v1_13_R1.Block;
import net.minecraft.server.v1_13_R1.BlockPosition;
import net.minecraft.server.v1_13_R1.Chunk;
import net.minecraft.server.v1_13_R1.ChunkConverter;
import net.minecraft.server.v1_13_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_13_R1.ChunkSection;
import net.minecraft.server.v1_13_R1.ChunkStatus;
import net.minecraft.server.v1_13_R1.Entity;
import net.minecraft.server.v1_13_R1.EntityTypes;
import net.minecraft.server.v1_13_R1.FluidType;
import net.minecraft.server.v1_13_R1.FluidTypes;
import net.minecraft.server.v1_13_R1.GeneratorAccess;
import net.minecraft.server.v1_13_R1.HeightMap;
import net.minecraft.server.v1_13_R1.IChunkAccess;
import net.minecraft.server.v1_13_R1.IChunkLoader;
import net.minecraft.server.v1_13_R1.NBTTagCompound;
import net.minecraft.server.v1_13_R1.ProtoChunk;
import net.minecraft.server.v1_13_R1.ProtoChunkTickList;
import net.minecraft.server.v1_13_R1.TileEntity;
import net.minecraft.server.v1_13_R1.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class CustomChunkLoader implements IChunkLoader {

    private static final Logger LOGGER = LogManager.getLogger("SWM Chunk Loader");

    private final Map<Long, Chunk> chunks = new HashMap<>();
    private final CraftSlimeWorld world;

    void loadAllChunks(CustomWorldServer server) {
        for (SlimeChunk chunk : world.getChunks().values()) {
            Chunk nmsChunk = createChunk(server, chunk);

            long index = (((long) chunk.getZ()) * Integer.MAX_VALUE + ((long) chunk.getX()));
            chunks.put(index, nmsChunk);
        }

        // Now that we've converted every SlimeChunk to its nms counterpart, we can empty the chunk list
        world.clearChunks();
    }

    Chunk[] getChunks() {
        synchronized (chunks) {
            return chunks.values().toArray(new Chunk[0]);
        }
    }

    private Chunk createChunk(CustomWorldServer server, SlimeChunk chunk) {
        int x = chunk.getX();
        int z = chunk.getZ();

        LOGGER.debug("Loading chunk (" + x + ", " + z + ") on world " + world.getName());

        BlockPosition.MutableBlockPosition mutableBlockPosition = new BlockPosition.MutableBlockPosition();

        // ProtoChunkTickLists
        ProtoChunkTickList<Block> airChunkTickList = new ProtoChunkTickList<>((block) -> block.getBlockData().isAir(), Block.REGISTRY::b, Block.REGISTRY::get, new ChunkCoordIntPair(x, z));
        ProtoChunkTickList<FluidType> fluidChunkTickList = new ProtoChunkTickList<>((fluidType) -> fluidType == FluidTypes.a, FluidType.c::b, FluidType.c::get, new ChunkCoordIntPair(x, z));

        // Biomes
        BiomeBase[] biomeBaseArray = new BiomeBase[256];
        int[] biomeIntArray = chunk.getBiomes();

        for (int i = 0; i < biomeIntArray.length; i++) {
            biomeBaseArray[i] = BiomeBase.a(biomeIntArray[i]);

            if (biomeBaseArray[i] == null) {
                biomeBaseArray[i] = server.getChunkProvider().getChunkGenerator().getWorldChunkManager().getBiome(mutableBlockPosition
                        .c((i & 15) + (x << 4), 0, (i >> 4 & 15) + (z << 4)), Biomes.c);
            }
        }

        CompoundTag upgradeDataTag = ((CraftSlimeChunk) chunk).getUpgradeData();
        Chunk nmsChunk = new Chunk(server, x, z, biomeBaseArray, upgradeDataTag == null ? ChunkConverter.a
                : new ChunkConverter((NBTTagCompound) Converter.convertTag(upgradeDataTag)), airChunkTickList, fluidChunkTickList, 0L);

        // Chunk status
        nmsChunk.c("postprocessed");

        // Chunk sections
        LOGGER.debug("Loading chunk sections for chunk (" + x + ", " + z + ") on world " + world.getName());
        ChunkSection[] sections = new ChunkSection[16];

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            SlimeChunkSection slimeSection = chunk.getSections()[sectionId];

            if (slimeSection != null && slimeSection.getBlockStates().length > 0) { // If block states array is empty, it's just a fake chunk made by the createEmptyWorld method
                ChunkSection section = new ChunkSection(sectionId, true);

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

        // Load tile entities
        LOGGER.debug("Loading tile entities for chunk (" + x + ", " + z + ") on world " + world.getName());
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

        LOGGER.debug("Loaded " + loadedEntities + " tile entities for chunk (" + x + ", " + z + ") on world " + world.getName());

        // Load entities
        LOGGER.debug("Loading entities for chunk (" + x + ", " + z + ") on world " + world.getName());
        List<CompoundTag> entities = chunk.getEntities();
        loadedEntities = 0;

        if (entities != null) {
            for (CompoundTag tag : entities) {
                loadEntity(tag, server.getMinecraftWorld(), nmsChunk);
            }
        }

        LOGGER.debug("Loaded " + loadedEntities + " entities for chunk (" + x + ", " + z + ") on world " + world.getName());
        LOGGER.debug("Loaded chunk (" + x + ", " + z + ") on world " + world.getName());

        return nmsChunk;
    }

    private Entity loadEntity(CompoundTag tag, World world, Chunk chunk) {
        Entity entity = EntityTypes.a((NBTTagCompound) Converter.convertTag(tag), world);
        chunk.f(true);

        if (entity != null) {
            chunk.a(entity);

            CompoundMap map = tag.getValue();

            if (map.containsKey("Passengers")) {
                List<CompoundTag> passengersList = (List<CompoundTag>) map.get("Passengers").getValue();

                for (CompoundTag passengerTag : passengersList) {
                    Entity passenger = loadEntity(passengerTag, world, chunk);

                    if (passengerTag != null) {
                        passenger.a(entity, true);
                    }
                }
            }
        }

        return entity;
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
        long index = (((long) z) * Integer.MAX_VALUE + ((long) x));
        Chunk chunk;

        synchronized (chunks) {
            chunk = chunks.get(index);
        }

        if (chunk == null) {
            BlockPosition.MutableBlockPosition mutableBlockPosition = new BlockPosition.MutableBlockPosition();

            // Biomes
            BiomeBase[] biomeBaseArray = new BiomeBase[256];

            for (int i = 0; i < biomeBaseArray.length; i++) {
                biomeBaseArray[i] = generatorAccess.getChunkProvider().getChunkGenerator().getWorldChunkManager().getBiome(mutableBlockPosition
                        .c((i & 15) + (x << 4), 0, (i >> 4 & 15) + (z << 4)), Biomes.c);
            }

            // ProtoChunkTickLists
            ProtoChunkTickList<Block> airChunkTickList = new ProtoChunkTickList<>((block) -> block.getBlockData().isAir(), Block.REGISTRY::b, Block.REGISTRY::get, new ChunkCoordIntPair(x, z));
            ProtoChunkTickList<FluidType> fluidChunkTickList = new ProtoChunkTickList<>((fluidType) -> fluidType == FluidTypes.a, FluidType.c::b, FluidType.c::get, new ChunkCoordIntPair(x, z));

            chunk = new Chunk(generatorAccess.getMinecraftWorld(), x, z, biomeBaseArray, ChunkConverter.a, airChunkTickList, fluidChunkTickList, 0L);
            chunk.c("postprocessed");
        }


        // Chunk consumer
        if (consumer != null) {
            consumer.accept(chunk);
        }

        return chunk;
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

        boolean empty = true;

        for (int sectionId = 0; sectionId < chunkAccess.getSections().length; sectionId++) {
            ChunkSection section = chunkAccess.getSections()[sectionId];

            if (section != null) {
                section.recalcBlockCounts();

                if (!section.a()) {
                    empty = false;
                    break;
                }
            }
        }

        long index = (((long) chunkAccess.getPos().z) * Integer.MAX_VALUE + ((long) chunkAccess.getPos().x));

        synchronized (chunks) {
            if (empty) {
                chunks.remove(index);
            } else {
                chunks.putIfAbsent(index, (Chunk) chunkAccess);
            }
        }
    }

    // Save all chunks
    @Override
    public void c() {

    }

    @Override
    public boolean chunkExists(int x, int z) {
        return true;
    }

    // Does literally nothing
    @Override
    public void b() {  }
}
