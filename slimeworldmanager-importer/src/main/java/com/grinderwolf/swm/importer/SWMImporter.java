/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.importer;

import com.flowpowered.nbt.*;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;
import com.github.luben.zstd.Zstd;
import com.github.tomaslanger.chalk.Chalk;
import com.grinderwolf.swm.api.exceptions.InvalidWorldException;
import com.grinderwolf.swm.api.utils.NibbleArray;
import com.grinderwolf.swm.api.utils.SlimeFormat;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.nms.CraftSlimeChunk;
import com.grinderwolf.swm.nms.CraftSlimeChunkSection;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class SWMImporter {

    private static final Pattern MAP_FILE_PATTERN = Pattern.compile("^(?:map_([0-9]*).dat)$");
    private static final int SECTOR_SIZE = 4096;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java -jar slimeworldmanager-importer-1.0.0.jar <path-to-world-folder>");

            return;
        }

        File worldDir = new File(args[0]);

        if (!worldDir.exists()) {
            System.err.println("World does not exist!");
            System.err.println("Provided path: " + args[0]);

            return;
        }

        if (!worldDir.isDirectory()) {
            System.err.println("Provided world path points out to a file, not to a directory!");

            return;
        }

        File regionDir = new File(worldDir, "region");

        if (!regionDir.exists() || !regionDir.isDirectory() || regionDir.list().length == 0) {
            System.err.println("Provided world seems to be corrupted.");

            return;
        }

        System.out.println("**** WARNING ****");
        System.out.println(
                "The Slime Format is meant to be used on tiny maps, not big survival worlds. It is recommended "
                        + "to trim your world by using the Prune MCEdit tool to ensure you don't save more chunks than you want to.");
        System.out.println();
        System.out.println("NOTE: This utility will automatically ignore every chunk that doesn't contain any blocks.");
        System.out.print("Do you want to continue? [Y/N]: ");

        Scanner scanner = new Scanner(System.in);
        String response = scanner.next();

        if (response.equalsIgnoreCase("Y")) {
            System.out.println("Loading world...");
            File levelFile = new File(worldDir, "level.dat");

            if (!levelFile.exists() || !levelFile.isFile()) {
                System.err.println("Provided world seems to be corrupted.");

                return;
            }

            LevelData data;

            try {
                data = readLevelData(levelFile);
            } catch (IOException ex) {
                System.err.println("Failed to load world level file:");
                ex.printStackTrace();
                return;
            } catch (InvalidWorldException ex) {
                System.err.println("Failed to find world version.");
                return;
            }

            // World version
            byte worldVersion;

            if (data.getVersion() == -1) { // DataVersion tag was added in 1.9
                worldVersion = 0x01;
            } else if (data.getVersion() < 818) {
                worldVersion = 0x02; // 1.9 world
            } else if (data.getVersion() < 1501) {
                worldVersion = 0x03; // 1.11 world
            } else if (data.getVersion() < 1517) {
                worldVersion = 0x04; // 1.13 world
            } else {
                worldVersion = 0x05; // 1.14 world
            }

            System.out.println("World version: " + worldVersion);
            List<SlimeChunk> chunks = new ArrayList<>();

            for (File file : regionDir.listFiles((dir, name) -> name.endsWith(".mca"))) {
                try {
                    chunks.addAll(loadChunks(file, worldVersion));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            System.out.println("World " + worldDir.getName() + " contains " + chunks.size() + " chunks.");

            // World maps
            File dataDir = new File(worldDir, "data");
            List<CompoundTag> maps = new ArrayList<>();

            if (dataDir.exists()) {
                if (!dataDir.isDirectory()) {
                    System.err.println("Failed to find world maps.");
                    return;
                }

                try {
                    for (File mapFile : dataDir.listFiles(
                            (dir, name) -> MAP_FILE_PATTERN.matcher(name).matches())) {
                        maps.add(loadMap(mapFile));
                    }
                } catch (IOException ex) {
                    System.err.println("Failed to read world maps.");
                    return;
                }
            }

            try {
                long start = System.currentTimeMillis();
                byte[] slimeFormattedWorld = generateSlimeWorld(chunks, worldVersion, data, maps);

                System.out.println(Chalk.on("World "
                                + worldDir.getName()
                                + " successfully serialized to the Slime Format in "
                                + (System.currentTimeMillis() - start)
                                + "ms!")
                        .green());

                File slimeFile = new File(worldDir.getName() + ".slime");

                slimeFile.createNewFile();

                try (FileOutputStream stream = new FileOutputStream(slimeFile)) {
                    stream.write(slimeFormattedWorld);
                    stream.flush();
                }
            } catch (IndexOutOfBoundsException ex) {
                // Thanks for providing a world so big that it just overflowed the coordinate
                // system!
                System.err.println(
                        "Hey! Didn't you just read the warning? The Slime Format isn't meant for big worlds. The world you provided "
                                + "just breaks everything. Please, trim it by using the MCEdit tool and try again.");
            } catch (IOException ex) {
                System.err.println("Failed to save the world file.");
                ex.printStackTrace();
            }
        }
    }

    private static LevelData readLevelData(File file) throws IOException, InvalidWorldException {
        NBTInputStream nbtStream = new NBTInputStream(new FileInputStream(file));
        Optional<CompoundTag> tag = nbtStream.readTag().getAsCompoundTag();

        if (tag.isPresent()) {
            Optional<CompoundTag> dataTag = tag.get().getAsCompoundTag("Data");

            if (dataTag.isPresent()) {
                // Data version
                int dataVersion = dataTag.get().getIntValue("DataVersion").orElse(-1);

                // Game rules
                Map<String, String> gameRules = new HashMap<>();
                Optional<CompoundTag> rulesList = dataTag.get().getAsCompoundTag("GameRules");

                rulesList.ifPresent(compoundTag -> compoundTag
                        .getValue()
                        .forEach((ruleName, ruleTag) -> gameRules.put(
                                ruleName, ruleTag.getAsStringTag().get().getValue())));

                return new LevelData(dataVersion, gameRules);
            }
        }

        throw new InvalidWorldException(file.getParentFile());
    }

    private static CompoundTag loadMap(File mapFile) throws IOException {
        String fileName = mapFile.getName();
        int mapId = Integer.parseInt(fileName.substring(4, fileName.length() - 4));

        NBTInputStream nbtStream =
                new NBTInputStream(new FileInputStream(mapFile), NBTInputStream.GZIP_COMPRESSION, ByteOrder.BIG_ENDIAN);
        CompoundTag tag = nbtStream
                .readTag()
                .getAsCompoundTag()
                .get()
                .getAsCompoundTag("data")
                .get();
        tag.getValue().put("id", new IntTag("id", mapId));

        return tag;
    }

    private static List<SlimeChunk> loadChunks(File file, byte worldVersion) throws IOException {
        System.out.println("Loading chunks from region file '" + file.getName() + "':");
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

        List<SlimeChunk> loadedChunks = chunks.stream()
                .map((entry) -> {
                    try {
                        DataInputStream headerStream = new DataInputStream(
                                new ByteArrayInputStream(regionByteArray, entry.getOffset(), entry.getPaddedSize()));

                        int chunkSize = headerStream.readInt() - 1;
                        int compressionScheme = headerStream.readByte();

                        DataInputStream chunkStream = new DataInputStream(
                                new ByteArrayInputStream(regionByteArray, entry.getOffset() + 5, chunkSize));
                        InputStream decompressorStream = compressionScheme == 1
                                ? new GZIPInputStream(chunkStream)
                                : new InflaterInputStream(chunkStream);
                        NBTInputStream nbtStream = new NBTInputStream(
                                decompressorStream, NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN);
                        CompoundTag globalCompound = (CompoundTag) nbtStream.readTag();
                        CompoundMap globalMap = globalCompound.getValue();

                        if (!globalMap.containsKey("Level")) {
                            throw new RuntimeException("Missing Level tag?");
                        }

                        CompoundTag levelCompound = (CompoundTag) globalMap.get("Level");

                        return readChunk(levelCompound, worldVersion);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        System.out.println(loadedChunks.size() + " chunks loaded.");

        return loadedChunks;
    }

    private static SlimeChunk readChunk(CompoundTag compound, byte worldVersion) {
        int chunkX = compound.getAsIntTag("xPos").get().getValue();
        int chunkZ = compound.getAsIntTag("zPos").get().getValue();
        Optional<String> status = compound.getStringValue("Status");

        if (status.isPresent()
                && !status.get().equals("postprocessed")
                && !status.get().startsWith("full")) {
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

        if (worldVersion >= 0x04) {
            heightMapsCompound = optionalHeightMaps.orElse(new CompoundTag("", new CompoundMap()));
        } else {
            // Pre 1.13 world

            int[] heightMap = compound.getIntArrayValue("HeightMap").orElse(new int[256]);
            heightMapsCompound = new CompoundTag("", new CompoundMap());
            heightMapsCompound.getValue().put("heightMap", new IntArrayTag("heightMap", heightMap));
        }

        List<CompoundTag> tileEntities = ((ListTag<CompoundTag>) compound.getAsListTag("TileEntities")
                        .orElse(new ListTag<>("TileEntities", TagType.TAG_COMPOUND, new ArrayList<>())))
                .getValue();
        List<CompoundTag> entities = ((ListTag<CompoundTag>) compound.getAsListTag("Entities")
                        .orElse(new ListTag<>("Entities", TagType.TAG_COMPOUND, new ArrayList<>())))
                .getValue();
        ListTag<CompoundTag> sectionsTag =
                (ListTag<CompoundTag>) compound.getAsListTag("Sections").get();
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

            if (worldVersion < 0x04) {
                dataArray = new NibbleArray(sectionTag.getByteArrayValue("Data").get());

                if (isEmpty(blocks)) { // Just skip it
                    continue;
                }

                paletteTag = null;
                blockStatesArray = null;
            } else {
                dataArray = null;

                paletteTag = (ListTag<CompoundTag>)
                        sectionTag.getAsListTag("Palette").orElse(null);
                blockStatesArray = sectionTag.getLongArrayValue("BlockStates").orElse(null);

                if (paletteTag == null || blockStatesArray == null || isEmpty(blockStatesArray)) { // Skip it
                    continue;
                }
            }

            NibbleArray blockLightArray = sectionTag.getValue().containsKey("BlockLight")
                    ? new NibbleArray(sectionTag.getByteArrayValue("BlockLight").get())
                    : null;
            NibbleArray skyLightArray = sectionTag.getValue().containsKey("SkyLight")
                    ? new NibbleArray(sectionTag.getByteArrayValue("SkyLight").get())
                    : null;

            sectionArray[index] = new CraftSlimeChunkSection(
                    blocks, dataArray, paletteTag, blockStatesArray, blockLightArray, skyLightArray);
        }

        for (SlimeChunkSection section : sectionArray) {
            if (section != null) { // Chunk isn't empty
                return new CraftSlimeChunk(
                        null, chunkX, chunkZ, sectionArray, heightMapsCompound, biomes, tileEntities, entities);
            }
        }

        // Chunk is empty
        return null;
    }

    private static int[] toIntArray(byte[] buf) {
        ByteBuffer buffer = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN);
        int[] ret = new int[buf.length / 4];

        buffer.asIntBuffer().get(ret);

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

    private static boolean isEmpty(long[] array) {
        for (long b : array) {
            if (b != 0L) {
                return false;
            }
        }

        return true;
    }

    private static byte[] generateSlimeWorld(
            List<SlimeChunk> chunks, byte worldVersion, LevelData levelData, List<CompoundTag> worldMaps) {
        List<SlimeChunk> sortedChunks = new ArrayList<>(chunks);
        sortedChunks.sort(
                Comparator.comparingLong(chunk -> (long) chunk.getZ() * Integer.MAX_VALUE + (long) chunk.getX()));

        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(outByteStream);

        try {
            // File Header and Slime version
            outStream.write(SlimeFormat.SLIME_HEADER);
            outStream.write(SlimeFormat.SLIME_VERSION);

            // World version
            outStream.writeByte(worldVersion);

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
            byte[] chunkData = serializeChunks(sortedChunks, worldVersion);
            byte[] compressedChunkData = Zstd.compress(chunkData);

            outStream.writeInt(compressedChunkData.length);
            outStream.writeInt(chunkData.length);
            outStream.write(compressedChunkData);

            // Tile Entities
            List<CompoundTag> tileEntitiesList = sortedChunks.stream()
                    .flatMap(chunk -> chunk.getTileEntities().stream())
                    .collect(Collectors.toList());
            ListTag<CompoundTag> tileEntitiesNbtList = new ListTag<>("tiles", TagType.TAG_COMPOUND, tileEntitiesList);
            CompoundTag tileEntitiesCompound =
                    new CompoundTag("", new CompoundMap(Collections.singletonList(tileEntitiesNbtList)));
            byte[] tileEntitiesData = serializeCompoundTag(tileEntitiesCompound);
            byte[] compressedTileEntitiesData = Zstd.compress(tileEntitiesData);

            outStream.writeInt(compressedTileEntitiesData.length);
            outStream.writeInt(tileEntitiesData.length);
            outStream.write(compressedTileEntitiesData);

            // Entities
            List<CompoundTag> entitiesList = sortedChunks.stream()
                    .flatMap(chunk -> chunk.getEntities().stream())
                    .collect(Collectors.toList());

            outStream.writeBoolean(!entitiesList.isEmpty());

            if (!entitiesList.isEmpty()) {
                ListTag<CompoundTag> entitiesNbtList = new ListTag<>("entities", TagType.TAG_COMPOUND, entitiesList);
                CompoundTag entitiesCompound =
                        new CompoundTag("", new CompoundMap(Collections.singletonList(entitiesNbtList)));
                byte[] entitiesData = serializeCompoundTag(entitiesCompound);
                byte[] compressedEntitiesData = Zstd.compress(entitiesData);

                outStream.writeInt(compressedEntitiesData.length);
                outStream.writeInt(entitiesData.length);
                outStream.write(compressedEntitiesData);
            }

            // Extra Tag
            CompoundMap extraMap = new CompoundMap();

            if (!levelData.getGameRules().isEmpty()) {
                CompoundMap gamerules = new CompoundMap();
                levelData.getGameRules().forEach((rule, value) -> gamerules.put(rule, new StringTag(rule, value)));

                extraMap.put("gamerules", new CompoundTag("gamerules", gamerules));
            }

            byte[] extraData = serializeCompoundTag(new CompoundTag("", extraMap));
            byte[] compressedExtraData = Zstd.compress(extraData);

            outStream.writeInt(compressedExtraData.length);
            outStream.writeInt(extraData.length);
            outStream.write(compressedExtraData);

            // World Maps
            CompoundMap map = new CompoundMap();
            map.put("maps", new ListTag<>("maps", TagType.TAG_COMPOUND, worldMaps));

            CompoundTag mapsCompound = new CompoundTag("", map);

            byte[] mapArray = serializeCompoundTag(mapsCompound);
            byte[] compressedMapArray = Zstd.compress(mapArray);

            outStream.writeInt(compressedMapArray.length);
            outStream.writeInt(mapArray.length);
            outStream.write(compressedMapArray);
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

    private static byte[] serializeChunks(List<SlimeChunk> chunks, byte worldVersion) throws IOException {
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(16384);
        DataOutputStream outStream = new DataOutputStream(outByteStream);

        for (SlimeChunk chunk : chunks) {
            // Height Maps
            if (worldVersion >= 0x04) {
                byte[] heightMaps = serializeCompoundTag(chunk.getHeightMaps());
                outStream.writeInt(heightMaps.length);
                outStream.write(heightMaps);
            } else {
                int[] heightMap =
                        chunk.getHeightMaps().getIntArrayValue("heightMap").get();

                for (int i = 0; i < 256; i++) {
                    outStream.writeInt(heightMap[i]);
                }
            }

            // Biomes
            int[] biomes = chunk.getBiomes();
            if (worldVersion >= 0x04) {
                outStream.writeInt(biomes.length);
            }

            for (int biome : biomes) {
                outStream.writeInt(biome);
            }

            // Chunk sections
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

                // Block Light
                boolean hasBlockLight = section.getBlockLight() != null;
                outStream.writeBoolean(hasBlockLight);

                if (hasBlockLight) {
                    outStream.write(section.getBlockLight().getBacking());
                }

                // Block Data
                if (worldVersion >= 0x04) {
                    // Palette
                    List<CompoundTag> palette = section.getPalette().getValue();
                    outStream.writeInt(palette.size());

                    for (CompoundTag value : palette) {
                        byte[] serializedValue = serializeCompoundTag(value);

                        outStream.writeInt(serializedValue.length);
                        outStream.write(serializedValue);
                    }

                    // Block states
                    long[] blockStates = section.getBlockStates();

                    outStream.writeInt(blockStates.length);

                    for (long value : section.getBlockStates()) {
                        outStream.writeLong(value);
                    }
                } else {
                    outStream.write(section.getBlocks());
                    outStream.write(section.getData().getBacking());
                }

                // Sky Light
                boolean hasSkyLight = section.getSkyLight() != null;
                outStream.writeBoolean(hasSkyLight);

                if (hasSkyLight) {
                    outStream.write(section.getSkyLight().getBacking());
                }
            }
        }

        return outByteStream.toByteArray();
    }

    private static byte[] serializeCompoundTag(CompoundTag tag) throws IOException {
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        NBTOutputStream outStream =
                new NBTOutputStream(outByteStream, NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN);
        outStream.writeTag(tag);

        return outByteStream.toByteArray();
    }
}
