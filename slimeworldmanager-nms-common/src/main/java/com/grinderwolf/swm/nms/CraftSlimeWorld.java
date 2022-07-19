/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.nms;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.TagType;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;
import com.github.luben.zstd.Zstd;
import com.grinderwolf.swm.api.exceptions.WorldAlreadyExistsException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.utils.SlimeFormat;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Difficulty;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
public class CraftSlimeWorld implements SlimeWorld {

    private final String name;
    private final Map<Long, SlimeChunk> chunks;
    private final CompoundTag extraData;
    private final List<CompoundTag> worldMaps;
    private final SlimePropertyMap propertyMap;
    private final boolean readOnly;
    private final boolean locked;
    private SlimeLoader loader;
    private byte version;

    private static void writeBitSetAsBytes(DataOutputStream outStream, BitSet set, int fixedSize)
            throws IOException {
        byte[] array = set.toByteArray();
        outStream.write(array);

        int chunkMaskPadding = fixedSize - array.length;

        for (int i = 0; i < chunkMaskPadding; i++) {
            outStream.write(0);
        }
    }

    private static byte[] serializeChunks(List<SlimeChunk> chunks, byte worldVersion)
            throws IOException {
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(16384);
        DataOutputStream outStream = new DataOutputStream(outByteStream);

        for (SlimeChunk chunk : chunks) {
            // Height Maps
            if (worldVersion >= 0x04) {
                byte[] heightMaps = serializeCompoundTag(chunk.getHeightMaps());
                outStream.writeInt(heightMaps.length);
                outStream.write(heightMaps);
            } else {
                int[] heightMap = chunk.getHeightMaps().getIntArrayValue("heightMap").get();

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
        if (tag == null || tag.getValue().isEmpty()) {
            return new byte[0];
        }

        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        NBTOutputStream outStream =
                new NBTOutputStream(
                        outByteStream, NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN);
        outStream.writeTag(tag);

        return outByteStream.toByteArray();
    }

    @Override
    public SlimeChunk getChunk(int x, int z) {
        synchronized (chunks) {
            Long index = (((long) z) * Integer.MAX_VALUE + ((long) x));

            return chunks.get(index);
        }
    }

    public void updateChunk(SlimeChunk chunk) {
        if (!chunk.getWorldName().equals(getName())) {
            throw new IllegalArgumentException(
                    "Chunk ("
                            + chunk.getX()
                            + ", "
                            + chunk.getZ()
                            + ") belongs to world '"
                            + chunk.getWorldName()
                            + "', not to '"
                            + getName()
                            + "'!");
        }

        synchronized (chunks) {
            chunks.put(((long) chunk.getZ()) * Integer.MAX_VALUE + ((long) chunk.getX()), chunk);
        }
    }

    @Override
    public SlimeWorld clone(String worldName) {
        try {
            return clone(worldName, null);
        } catch (WorldAlreadyExistsException | IOException ignored) {
            return null; // Never going to happen
        }
    }

    // World Serialization methods

    @Override
    public SlimeWorld clone(String worldName, SlimeLoader loader)
            throws WorldAlreadyExistsException, IOException {
        return clone(worldName, loader, true);
    }

    @Override
    public SlimeWorld clone(String worldName, SlimeLoader loader, boolean lock)
            throws WorldAlreadyExistsException, IOException {
        if (name.equals(worldName)) {
            throw new IllegalArgumentException(
                    "The clone world cannot have the same name as the original world!");
        }

        if (worldName == null) {
            throw new IllegalArgumentException("The world name cannot be null!");
        }

        if (loader != null) {
            if (loader.worldExists(worldName)) {
                throw new WorldAlreadyExistsException(worldName);
            }
        }

        CraftSlimeWorld world;

        synchronized (chunks) {
            world =
                    new CraftSlimeWorld(
                            worldName,
                            new HashMap<>(chunks),
                            extraData.clone(),
                            new ArrayList<>(worldMaps),
                            propertyMap,
                            loader == null,
                            lock,
                            loader == null ? this.loader : loader,
                            version);
        }

        if (loader != null) {
            loader.saveWorld(worldName, world.serialize(), lock);
        }

        return world;
    }

    @Override
    public SlimeWorld.SlimeProperties getProperties() {
        return SlimeWorld.SlimeProperties.builder()
                .spawnX(
                        propertyMap.getInt(
                                com.grinderwolf.swm.api.world.properties.SlimeProperties.SPAWN_X))
                .spawnY(
                        propertyMap.getInt(
                                com.grinderwolf.swm.api.world.properties.SlimeProperties.SPAWN_Y))
                .spawnZ(
                        propertyMap.getInt(
                                com.grinderwolf.swm.api.world.properties.SlimeProperties.SPAWN_Z))
                .environment(
                        propertyMap.getString(
                                com.grinderwolf.swm.api.world.properties.SlimeProperties
                                        .ENVIRONMENT))
                .pvp(
                        propertyMap.getBoolean(
                                com.grinderwolf.swm.api.world.properties.SlimeProperties.PVP))
                .allowMonsters(
                        propertyMap.getBoolean(
                                com.grinderwolf.swm.api.world.properties.SlimeProperties
                                        .ALLOW_MONSTERS))
                .allowAnimals(
                        propertyMap.getBoolean(
                                com.grinderwolf.swm.api.world.properties.SlimeProperties
                                        .ALLOW_ANIMALS))
                .difficulty(
                        Difficulty.valueOf(
                                        propertyMap
                                                .getString(
                                                        com.grinderwolf.swm.api.world.properties
                                                                .SlimeProperties.DIFFICULTY)
                                                .toUpperCase())
                                .getValue())
                .readOnly(readOnly)
                .build();
    }

    public byte[] serialize() {
        List<SlimeChunk> sortedChunks;

        synchronized (chunks) {
            sortedChunks = new ArrayList<>(chunks.values());
        }

        sortedChunks.sort(
                Comparator.comparingLong(
                        chunk -> (long) chunk.getZ() * Integer.MAX_VALUE + (long) chunk.getX()));
        sortedChunks.removeIf(
                chunk ->
                        chunk == null
                                || Arrays.stream(chunk.getSections())
                                        .allMatch(Objects::isNull)); // Remove empty chunks to save
        // space

        // Store world properties
        extraData.getValue().put("properties", propertyMap.toCompound());

        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(outByteStream);

        try {
            // File Header and Slime version
            outStream.write(SlimeFormat.SLIME_HEADER);
            outStream.write(SlimeFormat.SLIME_VERSION);

            // World version
            outStream.writeByte(version);

            // Lowest chunk coordinates
            int minX = sortedChunks.stream().mapToInt(SlimeChunk::getX).min().orElse(0);
            int minZ = sortedChunks.stream().mapToInt(SlimeChunk::getZ).min().orElse(0);
            int maxX = sortedChunks.stream().mapToInt(SlimeChunk::getX).max().orElse(0);
            int maxZ = sortedChunks.stream().mapToInt(SlimeChunk::getZ).max().orElse(0);

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
            byte[] chunkData = serializeChunks(sortedChunks, version);
            byte[] compressedChunkData = Zstd.compress(chunkData);

            outStream.writeInt(compressedChunkData.length);
            outStream.writeInt(chunkData.length);
            outStream.write(compressedChunkData);

            // Tile Entities
            List<CompoundTag> tileEntitiesList =
                    sortedChunks.stream()
                            .flatMap(chunk -> chunk.getTileEntities().stream())
                            .collect(Collectors.toList());
            ListTag<CompoundTag> tileEntitiesNbtList =
                    new ListTag<>("tiles", TagType.TAG_COMPOUND, tileEntitiesList);
            CompoundTag tileEntitiesCompound =
                    new CompoundTag(
                            "", new CompoundMap(Collections.singletonList(tileEntitiesNbtList)));
            byte[] tileEntitiesData = serializeCompoundTag(tileEntitiesCompound);
            byte[] compressedTileEntitiesData = Zstd.compress(tileEntitiesData);

            outStream.writeInt(compressedTileEntitiesData.length);
            outStream.writeInt(tileEntitiesData.length);
            outStream.write(compressedTileEntitiesData);

            // Entities
            List<CompoundTag> entitiesList =
                    sortedChunks.stream()
                            .flatMap(chunk -> chunk.getEntities().stream())
                            .collect(Collectors.toList());

            outStream.writeBoolean(!entitiesList.isEmpty());

            if (!entitiesList.isEmpty()) {
                ListTag<CompoundTag> entitiesNbtList =
                        new ListTag<>("entities", TagType.TAG_COMPOUND, entitiesList);
                CompoundTag entitiesCompound =
                        new CompoundTag(
                                "", new CompoundMap(Collections.singletonList(entitiesNbtList)));
                byte[] entitiesData = serializeCompoundTag(entitiesCompound);
                byte[] compressedEntitiesData = Zstd.compress(entitiesData);

                outStream.writeInt(compressedEntitiesData.length);
                outStream.writeInt(entitiesData.length);
                outStream.write(compressedEntitiesData);
            }

            // Extra Tag
            byte[] extra = serializeCompoundTag(extraData);
            byte[] compressedExtra = Zstd.compress(extra);

            outStream.writeInt(compressedExtra.length);
            outStream.writeInt(extra.length);
            outStream.write(compressedExtra);

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
}
