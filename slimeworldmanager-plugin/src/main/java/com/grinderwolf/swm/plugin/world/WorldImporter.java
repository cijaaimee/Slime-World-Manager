package com.grinderwolf.swm.plugin.world;

import com.flowpowered.nbt.ByteArrayTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntArrayTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.TagType;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.grinderwolf.swm.api.exceptions.InvalidWorldException;
import com.grinderwolf.swm.api.utils.NibbleArray;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.nms.CraftSlimeChunk;
import com.grinderwolf.swm.nms.CraftSlimeChunkSection;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class WorldImporter {

    private static final int SECTOR_SIZE = 4096;

    public static CraftSlimeWorld readFromDirectory(File worldDir) throws InvalidWorldException, IOException {
        File regionDir = new File(worldDir, "region");

        if (!regionDir.exists() || !regionDir.isDirectory()) {
            throw new InvalidWorldException(worldDir);
        }

        Map<Long, SlimeChunk> chunks = new HashMap<>();

        for (File file : regionDir.listFiles((dir, name) -> name.endsWith(".mca"))) {
            chunks.putAll(loadChunks(file).stream().collect(Collectors.toMap((chunk) -> ((long) chunk.getZ()) * Integer.MAX_VALUE + ((long) chunk.getX()), (chunk) -> chunk)));
        }

        if (chunks.isEmpty()) {
            throw new InvalidWorldException(worldDir);
        }

        boolean v1_13World = false;

        mainLoop:
        for (SlimeChunk chunk : chunks.values()) {
            for (SlimeChunkSection section : chunk.getSections()) {
                if (section != null) {
                    v1_13World = section.getBlocks() == null;

                    break mainLoop;
                }
            }
        }

        return new CraftSlimeWorld(null, worldDir.getName(), chunks, new CompoundTag("", new CompoundMap()), v1_13World, SlimeWorld.SlimeProperties.builder().build());
    }

    private static List<SlimeChunk> loadChunks(File file) throws IOException {
        byte[] regionByteArray = Files.readAllBytes(file.toPath());
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(regionByteArray));

        List<ChunkEntry> chunks = new ArrayList<>(1024);

        for (int i = 0; i < 1024; i++) {
            int entry = inputStream.readInt();
            int chunkOffset = entry >>> 8;
            int chunkSize = entry & 15;

            if (entry != 0) {
                ChunkEntry chunkEntry = new ChunkEntry(chunkOffset * SECTOR_SIZE, chunkSize * SECTOR_SIZE);
                chunks.add(chunkEntry);
            }
        }

        List<SlimeChunk> loadedChunks = chunks.stream().map((entry) -> {

            try {
                DataInputStream headerStream = new DataInputStream(new ByteArrayInputStream(regionByteArray, entry.getOffset(), entry.getPaddedSize()));

                int chunkSize = headerStream.readInt() - 1;
                int compressionScheme = headerStream.readByte();

                DataInputStream chunkStream = new DataInputStream(new ByteArrayInputStream(regionByteArray, entry.getOffset() + 5, chunkSize));
                InputStream decompressorStream = compressionScheme == 1 ? new GZIPInputStream(chunkStream) : new InflaterInputStream(chunkStream);
                NBTInputStream nbtStream = new NBTInputStream(decompressorStream, NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN);
                CompoundTag globalCompound = (CompoundTag) nbtStream.readTag();
                CompoundMap globalMap = globalCompound.getValue();

                if (!globalMap.containsKey("Level")) {
                    throw new RuntimeException("Missing Level tag?");
                }

                CompoundTag levelCompound = (CompoundTag) globalMap.get("Level");

                return readChunk(levelCompound);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        }).filter(Objects::nonNull).collect(Collectors.toList());

        return loadedChunks;
    }

    private static SlimeChunk readChunk(CompoundTag compound) {
        int chunkX = compound.getAsIntTag("xPos").get().getValue();
        int chunkZ = compound.getAsIntTag("zPos").get().getValue();
        Optional<String> status = compound.getStringValue("Status");

        if (status.isPresent() && !status.get().equals("postprocessed") && !status.get().startsWith("full")) {
            // It's a protochunk
            return null;
        }

        int[] biomes;
        Tag biomesTag = compound.getValue().get("Biomes");

        if (biomesTag instanceof IntArrayTag) {
            biomes = ((IntArrayTag) biomesTag).getValue();
        } else if (biomesTag instanceof ByteArrayTag) {
            byte[] byteBiomes = ((ByteArrayTag) biomesTag).getValue();
            biomes = toIntArray(byteBiomes);
        } else {
            biomes = null;
        }

        Optional<CompoundTag> optionalHeightMaps = compound.getAsCompoundTag("Heightmaps");
        CompoundTag heightMapsCompound;

        if (optionalHeightMaps.isPresent()) {
            heightMapsCompound = optionalHeightMaps.get();
        } else {
            // Pre 1.13 world

            int[] heightMap = compound.getIntArrayValue("HeightMap").orElse(new int[256]);
            heightMapsCompound = new CompoundTag("", new CompoundMap());
            heightMapsCompound.getValue().put("heightMap", new IntArrayTag("heightMap", heightMap));
        }

        List<CompoundTag> tileEntities = ((ListTag<CompoundTag>) compound.getAsListTag("TileEntities")
                .orElse(new ListTag<>("Entities", TagType.TAG_COMPOUND, new ArrayList<>()))).getValue();
        List<CompoundTag> entities = ((ListTag<CompoundTag>) compound.getAsListTag("Entities")
                .orElse(new ListTag<>("Entities", TagType.TAG_COMPOUND, new ArrayList<>()))).getValue();
        ListTag<CompoundTag> sectionsTag = (ListTag<CompoundTag>) compound.getAsListTag("Sections").get();
        SlimeChunkSection[] sectionArray = new SlimeChunkSection[16];

        for (CompoundTag sectionTag : sectionsTag.getValue()) {
            int index = sectionTag.getByteValue("Y").get();

            if (index < 0) {
                // For some reason MC 1.14 worlds contain an empty section with Y = -1.
                continue;
            }

            byte[] blocks = sectionTag.getByteArrayValue("Blocks").orElse(null);
            NibbleArray dataArray;
            ListTag<CompoundTag> paletteTag;
            long[] blockStatesArray;

            if (blocks != null) {
                // Pre 1.13 world
                dataArray = new NibbleArray(sectionTag.getByteArrayValue("Data").get());

                if (isEmpty(blocks)) { // Just skip it
                    continue;
                }

                paletteTag = null;
                blockStatesArray = null;
            } else {
                dataArray = null;

                paletteTag = (ListTag<CompoundTag>) sectionTag.getAsListTag("Palette").orElse(null);
                blockStatesArray = sectionTag.getLongArrayValue("BlockStates").orElse(null);

                if (paletteTag == null || blockStatesArray == null) { // Skip it
                    continue;
                }
            }

            NibbleArray blockLightArray= sectionTag.getValue().containsKey("BlockLight") ? new NibbleArray(sectionTag.getByteArrayValue("BlockLight").get()) : null;
            NibbleArray skyLightArray = sectionTag.getValue().containsKey("SkyLight") ? new NibbleArray(sectionTag.getByteArrayValue("SkyLight").get()) : null;

            sectionArray[index] = new CraftSlimeChunkSection(blocks, dataArray, paletteTag, blockStatesArray, blockLightArray, skyLightArray);
        }

        for (SlimeChunkSection section : sectionArray) {
            if (section != null) { // Chunk isn't empty
                return new CraftSlimeChunk(null, chunkX, chunkZ, sectionArray, heightMapsCompound, biomes, tileEntities, entities);
            }
        }

        // Chunk is empty

        return null;
    }

    private static int[] toIntArray(byte[] buf) {
        ByteBuffer buffer = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
        int[] ret = new int[buf.length / 4];

        buffer.asIntBuffer().put(ret);

        return ret;
    }

    private static boolean isEmpty(byte[] array) {
        for (byte b : array) {
            if (b != 0) {
                return false;
            }
        }

        return true;
    }

    @Getter
    @RequiredArgsConstructor
    private static class ChunkEntry {

        private final int offset;
        private final int paddedSize;

    }
}
