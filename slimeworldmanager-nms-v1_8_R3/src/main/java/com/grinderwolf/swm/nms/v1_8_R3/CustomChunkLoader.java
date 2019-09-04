package com.grinderwolf.swm.nms.v1_8_R3;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.utils.NibbleArray;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityTypes;
import net.minecraft.server.v1_8_R3.IChunkLoader;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.TileEntity;
import net.minecraft.server.v1_8_R3.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

@RequiredArgsConstructor
public class CustomChunkLoader implements IChunkLoader {

    private static final Logger LOGGER = LogManager.getLogger("SWM Chunk Loader");

    private final CraftSlimeWorld world;

    // Load chunk
    @Override
    public Chunk a(World nmsWorld, int x, int z) {
        LOGGER.debug("Loading chunk (" + x + ", " + z + ") on world " + world.getName());

        SlimeChunk chunk = world.getChunk(x, z);
        Chunk nmsChunk = new Chunk(nmsWorld, x, z);

        nmsChunk.d(true);
        nmsChunk.e(true);

        if (chunk == null) {
            long index = (((long) z) * Integer.MAX_VALUE + ((long) x));

            LOGGER.debug("Failed to load chunk (" + x + ", " + z + ") (" + index + ") on world " + world.getName() + ": chunk does not exist. Generating empty one...");

            return nmsChunk;
        }

        CompoundTag heightMapsCompound = chunk.getHeightMaps();
        int[] heightMap = heightMapsCompound.getIntArrayValue("heightMap").get();

        nmsChunk.a(heightMap);

        // Load chunk sections
        LOGGER.debug("Loading chunk sections for chunk (" + x + ", " + z + ") on world " + world.getName());
        ChunkSection[] sections = new ChunkSection[16];

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            SlimeChunkSection slimeSection = chunk.getSections()[sectionId];

            if (slimeSection != null) {
                ChunkSection section = new ChunkSection(sectionId << 4, true);
                NibbleArray data = slimeSection.getData();

                byte[] blocks = slimeSection.getBlocks();
                char[] blockIds = new char[blocks.length];

                for (int id = 0; id < blocks.length; id++) {
                    int blockId = blocks[id] & 255;
                    int blockData = data.get(id);
                    int packed = blockId << 4 | blockData;

                    if (Block.d.a(packed) == null) {
                        Block block = Block.getById(blockId);

                        if (block != null) {
                            try {
                                blockData = block.toLegacyData(block.fromLegacyData(blockData));
                            } catch (Exception ex) {
                                blockData = block.toLegacyData(block.getBlockData());
                            }

                            packed = blockId << 4 | blockData;
                        }
                    }

                    blockIds[id] = (char) packed;
                }

                LOGGER.debug("ChunkSection #" + sectionId + " - Chunk (" + x + ", " + z + ") - World " + world.getName() + ":");
                LOGGER.debug("Blocks:");
                LOGGER.debug(blockIds);
                LOGGER.debug("Block light array:");
                LOGGER.debug(slimeSection.getBlockLight().getBacking());
                LOGGER.debug("Sky light array:");
                LOGGER.debug(slimeSection.getSkyLight().getBacking());

                section.a(blockIds);
                section.a(Converter.convertArray(slimeSection.getBlockLight()));
                section.b(Converter.convertArray(slimeSection.getSkyLight()));

                section.recalcBlockCounts();
                sections[sectionId] = section;
            }
        }

        nmsChunk.a(sections);

        // Biomes
        nmsChunk.a(toByteArray(chunk.getBiomes()));

        // Load tile entities
        LOGGER.debug("Loading tile entities for chunk (" + x + ", " + z + ") on world " + world.getName());
        List<CompoundTag> tileEntities = chunk.getTileEntities();
        int loadedEntities = 0;

        if (tileEntities != null) {
            for (CompoundTag tag : tileEntities) {
                TileEntity entity = TileEntity.c((NBTTagCompound) Converter.convertTag(tag));

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
                Entity entity = EntityTypes.a((NBTTagCompound) Converter.convertTag(tag), nmsWorld);
                nmsChunk.g(true);

                if (entity != null) {
                    nmsChunk.a(entity);
                    Entity entity1 = entity;

                    for (CompoundTag ridingTag = tag; ridingTag.getValue().containsKey("Riding"); ridingTag = (CompoundTag) ridingTag.getValue().get("Riding")) {
                        Entity entity2 = EntityTypes.a((NBTTagCompound) Converter.convertTag(ridingTag.getValue().get("Riding")), nmsWorld);

                        if (entity2 != null) {
                            nmsChunk.a(entity2);
                            entity1.mount(entity2);
                            loadedEntities++;
                        }

                        entity1 = entity2;
                    }

                    loadedEntities++;
                }
            }
        }

        LOGGER.debug("Loaded " + loadedEntities + " entities for chunk (" + x + ", " + z + ") on world " + world.getName());
        LOGGER.debug("Loaded chunk (" + x + ", " + z + ") on world " + world.getName());

        return nmsChunk;
    }

    private byte[] toByteArray(int[] ints) {
        ByteBuffer buf = ByteBuffer.allocate(ints.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        buf.asIntBuffer().put(ints);

        return buf.array();
    }

    // Save chunk
    @Override
    public void a(World world, Chunk chunk) {
        SlimeChunk slimeChunk = Converter.convertChunk(chunk);
        this.world.updateChunk(slimeChunk);
    }


    // Save all chunks
    @Override
    public void b() {

    }

    // Does literally nothing
    @Override
    public void b(World world, Chunk chunk) { }

    // Does literally nothing
    @Override
    public void a() {  }
}
