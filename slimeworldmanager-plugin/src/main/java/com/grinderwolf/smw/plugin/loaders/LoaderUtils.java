package com.grinderwolf.smw.plugin.loaders;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;
import com.github.luben.zstd.Zstd;
import com.grinderwolf.smw.api.SlimeChunk;
import com.grinderwolf.smw.api.SlimeChunkSection;
import com.grinderwolf.smw.api.SlimeLoader;
import com.grinderwolf.smw.api.SlimeWorld;
import com.grinderwolf.smw.api.exceptions.CorruptedWorldException;
import com.grinderwolf.smw.api.utils.NibbleArray;
import com.grinderwolf.smw.nms.CraftSlimeChunk;
import com.grinderwolf.smw.nms.CraftSlimeChunkSection;
import com.grinderwolf.smw.nms.CraftSlimeWorld;
import org.bukkit.entity.Slime;

import java.io.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.stream.Collectors;

public class LoaderUtils {

    private static final byte[] SLIME_HEADER = new byte[] { -79, 11 };
    private static final int SLIME_VERSION = 3;

    public static SlimeWorld loadWorldFromStream(SlimeLoader loader, String worldName, DataInputStream dataStream) throws IOException, CorruptedWorldException {
        byte[] fileHeader = new byte[SLIME_HEADER.length];
        dataStream.read(fileHeader);

        if (!Arrays.equals(SLIME_HEADER, fileHeader)) {
            throw new CorruptedWorldException(worldName);
        }

        // File version
        byte version = dataStream.readByte();

        // Chunk
        short minX = dataStream.readShort();
        short minZ = dataStream.readShort();
        int width = dataStream.readShort();
        int depth = dataStream.readShort();

        int bitmaskSize = (int) Math.ceil((width * depth) / 8.0D);
        byte[] chunkBitmask = new byte[bitmaskSize];
        dataStream.read(chunkBitmask);
        BitSet chunkBitset = BitSet.valueOf(chunkBitmask);

        int compressedChunkDataLength = dataStream.readInt();
        int chunkDataLength = dataStream.readInt();
        byte[] compressedChunkData = new byte[compressedChunkDataLength];
        byte[] chunkData = new byte[chunkDataLength];

        dataStream.read(compressedChunkData);

        // Tile Entities
        int compressedTileEntitiesLength = dataStream.readInt();
        int tileEntitiesLength = dataStream.readInt();
        byte[] compressedTileEntities = new byte[compressedTileEntitiesLength];
        byte[] tileEntities = new byte[tileEntitiesLength];

        dataStream.read(compressedTileEntities);

        // Entities
        byte[] compressedEntities = new byte[0];
        byte[] entities = new byte[0];

        if (version >= 3) {
            boolean hasEntities = dataStream.readBoolean();

            if (hasEntities) {
                int compressedEntitiesLength = dataStream.readInt();
                int entitiesLength = dataStream.readInt();
                compressedEntities = new byte[compressedEntitiesLength];
                entities = new byte[entitiesLength];

                dataStream.read(compressedEntities);
            }
        }

        // Extra NBT tag
        byte[] compressedExtraTag = new byte[0];
        byte[] extraTag = new byte[0];

        if (version >= 2) {
            int compressedExtraTagLength = dataStream.readInt();
            int extraTagLength = dataStream.readInt();
            compressedExtraTag = new byte[compressedExtraTagLength];
            extraTag = new byte[extraTagLength];

            dataStream.read(compressedExtraTag);
        }

        if (dataStream.read() != -1) {
            throw new CorruptedWorldException(worldName);
        }

        // Data decompression
        Zstd.decompress(chunkData, compressedChunkData);
        Zstd.decompress(tileEntities, compressedTileEntities);
        Zstd.decompress(entities, compressedEntities);
        Zstd.decompress(extraTag, compressedExtraTag);

        // Chunk deserialization
        Map<Long, SlimeChunk> chunks = readChunks(worldName, minX, minZ, width, depth, chunkBitset, chunkData);

        // Entity deserialization
        // noinspection unchecked
        //CompoundTag entitiesCompound = readCompoundTag(entities);

        // Tile Entity deserialization
        // noinspection unchecked
        CompoundTag tileEntitiesCompound = readCompoundTag(tileEntities);

        if (tileEntitiesCompound != null) {
            ListTag<CompoundTag> tileEntitiesList = (ListTag<CompoundTag>) tileEntitiesCompound.getValue().get("tiles");
            System.out.println("Loading Tile Entities:");

            for (CompoundTag tileEntityCompound : tileEntitiesList.getValue()) {
                System.out.println(" - " + tileEntityCompound.toString());

                int chunkX = ((IntTag) tileEntityCompound.getValue().get("x")).getValue() >> 4;
                int chunkZ = ((IntTag) tileEntityCompound.getValue().get("z")).getValue() >> 4;
                long chunkKey = ((long) chunkZ) * Integer.MAX_VALUE + ((long) chunkX);
                SlimeChunk chunk = chunks.get(chunkKey);


                if (chunk == null) {
                    //throw new CorruptedWorldException(worldName);
                    continue;
                }

                chunk.getTileEntities().add(tileEntityCompound);
            }
        }

        return new CraftSlimeWorld(loader, worldName, chunks);
    }

    private static Map<Long, SlimeChunk> readChunks(String worldName, int minX, int minZ, int width, int depth, BitSet chunkBitset, byte[] chunkData) throws IOException {
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(chunkData));
        Map<Long, SlimeChunk> chunkMap = new HashMap<>();

        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                int bitsetIndex = z * width + x;

                if (chunkBitset.get(bitsetIndex)) {
                    // HeightMap
                    int[] heightMap = new int[256];

                    for (int i = 0; i < 256; i++) {
                        heightMap[i] = dataStream.readInt();
                    }

                    // Biome array
                    byte[] biomes = new byte[256];
                    dataStream.read(biomes);

                    // Chunk Sections
                    SlimeChunkSection[] sections = readChunkSections(dataStream);

                    chunkMap.put(((long) minZ + z) * Integer.MAX_VALUE + ((long) minX + x), new CraftSlimeChunk(worldName,minX + x, minZ + z,
                            sections, heightMap, biomes, new ArrayList<>(), new ArrayList<>()));
                }
            }
        }

        return chunkMap;
    }

    private static SlimeChunkSection[] readChunkSections(DataInputStream dataStream) throws IOException {
        SlimeChunkSection[] chunkSectionArray = new SlimeChunkSection[16];
        byte[] sectionBitmask = new byte[2];
        dataStream.read(sectionBitmask);
        BitSet sectionBitset = BitSet.valueOf(sectionBitmask);

        for (int i = 0; i < 16; i++) {
            if (sectionBitset.get(i)) {
                // Block Light Nibble Array
                byte[] blockLightByteArray = new byte[2048];
                dataStream.read(blockLightByteArray);
                NibbleArray blockLightArray = new NibbleArray((blockLightByteArray));

                // Block Array
                byte[] blockArray = new byte[4096];
                dataStream.read(blockArray);

                // Block Data Nibble Array
                byte[] dataByteArray = new byte[2048];
                dataStream.read(dataByteArray);
                NibbleArray dataArray = new NibbleArray((dataByteArray));

                // Sky Light Nibble Array
                byte[] skyLightByteArray = new byte[2048];
                dataStream.read(skyLightByteArray);
                NibbleArray skyLightArray = new NibbleArray((skyLightByteArray));

                // HypixelBlocks 3
                short hypixelBlocksLength = dataStream.readShort();
                dataStream.skip(hypixelBlocksLength);

                chunkSectionArray[i] = new CraftSlimeChunkSection(blockArray, dataArray, blockLightArray, skyLightArray);
            }
        }

        return chunkSectionArray;
    }

    private static CompoundTag readCompoundTag(byte[] serializedCompound) throws IOException {
        if (serializedCompound.length == 0) {
            return null;
        }

        NBTInputStream stream = new NBTInputStream(new ByteArrayInputStream(serializedCompound), false, ByteOrder.BIG_ENDIAN);

        return (CompoundTag) stream.readTag();
    }

    public static byte[] serializeWorld(CraftSlimeWorld world) {
        while (world.isSaving()) { }

        world.setSaving(true);
        List<SlimeChunk> sortedChunks = new ArrayList<>(world.getChunks().values());
        world.setSaving(false);

        sortedChunks.sort(Comparator.comparingLong(chunk -> (long) chunk.getZ() * Integer.MAX_VALUE + (long) chunk.getX()));
        sortedChunks.removeIf(chunk -> chunk == null || Arrays.stream(chunk.getSections()).allMatch(Objects::isNull)); // Remove empty chunks to save space


        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(outByteStream);

        try {
            // File Header and Slime version
            outStream.write(SLIME_HEADER);
            outStream.write(SLIME_VERSION);

            // Lowest chunk coordinates
            int minX = sortedChunks.stream().mapToInt(SlimeChunk::getX).min().getAsInt();
            int minZ = sortedChunks.stream().mapToInt(SlimeChunk::getZ).min().getAsInt();
            int maxX = sortedChunks.stream().mapToInt(SlimeChunk::getX).max().getAsInt();
            int maxZ = sortedChunks.stream().mapToInt(SlimeChunk::getZ).max().getAsInt();

            outStream.writeShort(minX);
            outStream.writeShort(minZ);

            // Width and depth
            int width = maxX - minX + 1;
            int depth = maxZ - minZ + 1;

            outStream.writeShort(width);
            outStream.writeShort(depth);

            // Chunk Bitmask
            BitSet chunkBitset = new BitSet(width * depth);

            for (SlimeChunk chunk : sortedChunks) {
                int bitsetIndex = (chunk.getZ() - minZ) * width + (chunk.getX() - minX);

                chunkBitset.set(bitsetIndex, true);
            }

            int chunkMaskSize = (int) Math.ceil((width * depth) / 8.0D);
            writeBitSetAsBytes(outStream, chunkBitset, chunkMaskSize);

            // Chunks
            byte[] chunkData = serializeChunks(sortedChunks);
            byte[] compressedChunkData = Zstd.compress(chunkData);

            outStream.writeInt(compressedChunkData.length);
            outStream.writeInt(chunkData.length);
            outStream.write(compressedChunkData);

            // Tile Entities
            List<CompoundTag> tileEntitiesList = sortedChunks.stream().flatMap(chunk -> chunk.getTileEntities().stream()).collect(Collectors.toList());
            ListTag<CompoundTag> tileEntitiesNbtList = new ListTag<>("tiles", CompoundTag.class, tileEntitiesList);
            CompoundTag tileEntitiesCompound = new CompoundTag("", new CompoundMap(Collections.singletonList(tileEntitiesNbtList)));
            byte[] tileEntitiesData = serializeCompoundTag(tileEntitiesCompound);
            byte[] compressedTileEntitiesData = Zstd.compress(tileEntitiesData);

            outStream.writeInt(compressedTileEntitiesData.length);
            outStream.writeInt(tileEntitiesData.length);
            outStream.write(compressedTileEntitiesData);

            // Entities
            List<CompoundTag> entitiesList = sortedChunks.stream().flatMap(chunk -> chunk.getEntities().stream()).collect(Collectors.toList());

            outStream.writeBoolean(!entitiesList.isEmpty());

            if (!entitiesList.isEmpty()) {
                ListTag<CompoundTag> entitiesNbtList = new ListTag<>("entities", CompoundTag.class, entitiesList);
                CompoundTag entitiesCompound = new CompoundTag("", new CompoundMap(Collections.singletonList(entitiesNbtList)));
                byte[] entitiesData = serializeCompoundTag(entitiesCompound);
                byte[] compressedEntitiesData = Zstd.compress(entitiesData);

                outStream.writeInt(compressedEntitiesData.length);
                outStream.writeInt(entitiesData.length);
                outStream.write(compressedEntitiesData);
            }

            // Extra Tag
            outStream.writeInt(0);
            outStream.writeInt(0);
        } catch (IOException ex) { // Ignore
            ex.printStackTrace();
        }

        return outByteStream.toByteArray();
    }

    private static void writeBitSetAsBytes(DataOutputStream outStream, BitSet set, int fixedSize) throws IOException {
        byte[] array = set.toByteArray();
        outStream.write(array);

        int chunkMaskPadding = fixedSize - array.length;

        for (int i = 0; i < chunkMaskPadding; i++) {
            outStream.write(0);
        }
    }

    private static byte[] serializeChunks(List<SlimeChunk> chunks) throws IOException {
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(16384);
        DataOutputStream outStream = new DataOutputStream(outByteStream);

        for (SlimeChunk chunk : chunks) {
            for (int value : chunk.getHeightMap()) {
                outStream.writeInt(value);
            }

            outStream.write(chunk.getBiomes());

            SlimeChunkSection[] sections = chunk.getSections();
            BitSet sectionBitmask = new BitSet(16);

            for (int i = 0; i < sections.length; i++) {
                sectionBitmask.set(i, sections[i] != null);
            }

            writeBitSetAsBytes(outStream, sectionBitmask, 2);

            for (SlimeChunkSection section : sections) {
                if (section == null) {
                    continue;
                }

                outStream.write(section.getBlockLight().getBacking());
                outStream.write(section.getBlocks());
                outStream.write(section.getData().getBacking());
                outStream.write(section.getSkyLight().getBacking());
                outStream.writeShort(0); // HypixelBlocks 3
            }
        }

        return outByteStream.toByteArray();
    }

    private static byte[] serializeCompoundTag(CompoundTag tag) throws IOException {
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        NBTOutputStream outStream = new NBTOutputStream(outByteStream, false, ByteOrder.BIG_ENDIAN);
        outStream.writeTag(tag);

        return outByteStream.toByteArray();
    }
}
