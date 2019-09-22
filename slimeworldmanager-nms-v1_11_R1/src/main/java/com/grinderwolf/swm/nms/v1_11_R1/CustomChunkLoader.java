package com.grinderwolf.swm.nms.v1_11_R1;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.utils.NibbleArray;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_11_R1.Chunk;
import net.minecraft.server.v1_11_R1.ChunkSection;
import net.minecraft.server.v1_11_R1.Entity;
import net.minecraft.server.v1_11_R1.EntityTypes;
import net.minecraft.server.v1_11_R1.IChunkLoader;
import net.minecraft.server.v1_11_R1.NBTTagCompound;
import net.minecraft.server.v1_11_R1.TileEntity;
import net.minecraft.server.v1_11_R1.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        Chunk nmsChunk = new Chunk(server, x, z);

        nmsChunk.d(true);
        nmsChunk.e(true);

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

                LOGGER.debug("ChunkSection #" + sectionId + " - Chunk (" + x + ", " + z + ") - World " + world.getName() + ":");
                LOGGER.debug("Blocks:");
                LOGGER.debug(blocks);
                LOGGER.debug("Block light array:");
                LOGGER.debug(slimeSection.getBlockLight().getBacking());
                LOGGER.debug("Sky light array:");
                LOGGER.debug(slimeSection.getSkyLight().getBacking());

                section.getBlocks().a(blocks, Converter.convertArray(data), new net.minecraft.server.v1_11_R1.NibbleArray());
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
                TileEntity entity = TileEntity.a(server, (NBTTagCompound) Converter.convertTag(tag));

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
                loadEntity(tag, server, nmsChunk);
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

    private Entity loadEntity(CompoundTag tag, World world, Chunk chunk) {
        Entity entity = EntityTypes.a((NBTTagCompound) Converter.convertTag(tag), world);
        chunk.g(true);

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

    // Load chunk
    @Override
    public Chunk a(World nmsWorld, int x, int z) {
        long index = (((long) z) * Integer.MAX_VALUE + ((long) x));

        Chunk chunk;

        synchronized (chunks) {
            chunk = chunks.get(index);
        }

        if (chunk == null) {
            chunk = new Chunk(nmsWorld, x, z);

            chunk.d(true);
            chunk.e(true);
        }

        return chunk;
    }

    // Save chunk
    @Override
    public void a(World world, Chunk chunk) {
        boolean empty = true;

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            ChunkSection section = chunk.getSections()[sectionId];

            if (section != null) {
                section.recalcBlockCounts();

                if (!section.a()) {
                    empty = false;
                    break;
                }
            }
        }

        long index = (((long) chunk.locZ) * Integer.MAX_VALUE + ((long) chunk.locX));

        synchronized (chunks) {
            if (empty) {
                chunks.remove(index);
            } else {
                chunks.putIfAbsent(index, chunk);
            }
        }
    }


    // Save all chunks
    @Override
    public void b() {

    }

    // Chunk exists
    @Override
    public boolean a(int i, int i1) {
        return true;
    }

    // Does literally nothing
    @Override
    public void b(World world, Chunk chunk) { }

    // Does literally nothing
    @Override
    public void a() {  }
}
