package com.grinderwolf.swm.plugin.upgrade.v1_13;

import com.flowpowered.nbt.ByteArrayTag;
import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.DoubleTag;
import com.flowpowered.nbt.FloatTag;
import com.flowpowered.nbt.IntArrayTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.LongTag;
import com.flowpowered.nbt.ShortTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.TagType;
import com.google.gson.GsonBuilder;
import com.grinderwolf.swm.api.utils.NibbleArray;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.nms.CraftSlimeChunk;
import com.grinderwolf.swm.nms.CraftSlimeChunkSection;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.log.Logging;
import com.grinderwolf.swm.plugin.upgrade.Upgrade;
import com.grinderwolf.swm.plugin.upgrade.v1_13.deserializers.BlockEntryDeserializer;
import com.grinderwolf.swm.plugin.upgrade.v1_13.deserializers.DowngradeDataDeserializer;
import com.grinderwolf.swm.plugin.upgrade.v1_13.deserializers.SetActionDeserializer;
import org.bukkit.ChatColor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class v1_13WorldUpgrade implements Upgrade {

    private DowngradeData downgradeData;

    @Override
    public void upgrade(CraftSlimeWorld world) {
        Logging.warning("Updating world to the 1.13 format. This may take a while.");

        List<SlimeChunk> chunks = new ArrayList<>(world.getChunks().values());
        long lastMessage = -1;

        for (int i = 0; i < chunks.size(); i++) {
            SlimeChunk chunk = chunks.get(i);

            // The world upgrade process is a very complex task, and there's already a
            // built-in upgrade tool inside the server, so we can simply use it
            CompoundTag globalTag = new CompoundTag("", new CompoundMap());
            globalTag.getValue().put("DataVersion", new IntTag("DataVersion", 1343));

            CompoundTag chunkTag = new CompoundTag("Level", new CompoundMap());

            chunkTag.getValue().put("xPos", new IntTag("xPos", chunk.getX()));
            chunkTag.getValue().put("zPos", new IntTag("zPos", chunk.getZ()));
            chunkTag.getValue().put("Sections", serializeSections(chunk.getSections()));
            chunkTag.getValue().put("Entities", new ListTag<>("Entities", TagType.TAG_COMPOUND, chunk.getEntities()));
            chunkTag.getValue().put("TileEntities", new ListTag<>("TileEntities", TagType.TAG_COMPOUND, chunk.getTileEntities()));
            chunkTag.getValue().put("TileTicks", new ListTag<>("TileTicks", TagType.TAG_COMPOUND, new ArrayList<>()));
            chunkTag.getValue().put("TerrainPopulated", new ByteTag("TerrainPopulated", (byte) 1));
            chunkTag.getValue().put("LightPopulated", new ByteTag("LightPopulated", (byte) 1));

            globalTag.getValue().put("Level", chunkTag);

            globalTag = SWMPlugin.getInstance().getNms().convertChunk(globalTag);
            chunkTag = globalTag.getAsCompoundTag("Level").get();

            // Chunk sections
            SlimeChunkSection[] newSections = new SlimeChunkSection[16];
            ListTag<CompoundTag> serializedSections = (ListTag<CompoundTag>) chunkTag.getAsListTag("Sections").get();

            for (CompoundTag sectionTag : serializedSections.getValue()) {
                ListTag<CompoundTag> palette = (ListTag<CompoundTag>) sectionTag.getAsListTag("Palette").get();
                long[] blockStates = sectionTag.getLongArrayValue("BlockStates").get();

                NibbleArray blockLight = new NibbleArray(sectionTag.getByteArrayValue("BlockLight").get());
                NibbleArray skyLight = new NibbleArray(sectionTag.getByteArrayValue("SkyLight").get());

                int index = sectionTag.getIntValue("Y").get();

                SlimeChunkSection section = new CraftSlimeChunkSection(null, null, palette, blockStates, blockLight, skyLight);
                newSections[index] = section;
            }

            // Biomes
            int[] newBiomes = new int[256];

            for (int index = 0; index < chunk.getBiomes().length; index++) {
                newBiomes[index] = chunk.getBiomes()[index] & 255;
            }

            // Upgrade data
            CompoundTag upgradeData = chunkTag.getAsCompoundTag("UpgradeData").orElse(null);

            // Chunk update
            SlimeChunk newChunk = new CraftSlimeChunk(world.getName(), chunk.getX(), chunk.getZ(), newSections,
                    new CompoundTag("", new CompoundMap()), newBiomes, chunk.getTileEntities(), chunk.getEntities(), upgradeData);

            world.updateChunk(newChunk);

            int done = i + 1;
            if (done == chunks.size()) {
                Logging.info(ChatColor.GREEN + "World successfully converted to the 1.13 format!");
            } else if (System.currentTimeMillis() - lastMessage > 1000) {
                int percentage = (done * 100) / chunks.size();
                Logging.info("Converting world... " + percentage + "%");
                lastMessage = System.currentTimeMillis();
            }
        }
    }

    private ListTag<CompoundTag> serializeSections(SlimeChunkSection[] sections) {
        ListTag<CompoundTag> sectionList = new ListTag<>("Sections", TagType.TAG_COMPOUND, new ArrayList<>());

        for (int i = 0; i < sections.length; i++) {
            SlimeChunkSection section = sections[i];

            if (section != null) {
                CompoundTag sectionTag = new CompoundTag(i + "", new CompoundMap());

                sectionTag.getValue().put("Y", new IntTag("Y", i));
                sectionTag.getValue().put("Blocks", new ByteArrayTag("Blocks", section.getBlocks()));
                sectionTag.getValue().put("Data", new ByteArrayTag("Data", section.getData().getBacking()));
                sectionTag.getValue().put("BlockLight", new ByteArrayTag("Data", section.getBlockLight().getBacking()));
                sectionTag.getValue().put("SkyLight", new ByteArrayTag("Data", section.getSkyLight().getBacking()));

                sectionList.getValue().add(sectionTag);
            }
        }

        return sectionList;
    }

    @Override
    public void downgrade(CraftSlimeWorld world) {
        if (downgradeData == null) {
            try {
                loadDowngradeData();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        Logging.warning("Downgrading world to the 1.12 format. This may take a while.");
        List<SlimeChunk> chunks = new ArrayList<>(world.getChunks().values());
        chunks.sort(Comparator.comparingLong(chunk -> (long) chunk.getZ() * Integer.MAX_VALUE + (long) chunk.getX()));

        long lastMessage = -1;

        for (int i = 0; i < chunks.size(); i++) {
            SlimeChunk chunk = chunks.get(i);
            SlimeChunkSection[] newSections = new SlimeChunkSection[16];

            for (int sectionIndex = 0; sectionIndex < 16; sectionIndex++) {
                SlimeChunkSection section = chunk.getSections()[sectionIndex];

                if (section != null) {
                    List<CompoundTag> palette = section.getPalette().getValue();
                    long[] blockData = section.getBlockStates();

                    byte[] blockArray = new byte[4096];
                    NibbleArray dataArray = new NibbleArray(4096);

                    int bitsPerBlock = Math.max(4, blockData.length * 64 / 4096);
                    long maxEntryValue = (1L << bitsPerBlock) - 1;

                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                int arrayIndex = y << 8 | z << 4 | x;
                                int bitIndex = arrayIndex * bitsPerBlock;
                                int startIndex = bitIndex / 64;
                                int endIndex = ((arrayIndex + 1) * bitsPerBlock - 1) / 64;
                                int startBitSubIndex = bitIndex % 64;

                                int val;

                                if (startIndex == endIndex) {
                                    val = (int) (blockData[startIndex] >>> startBitSubIndex & maxEntryValue);
                                } else {
                                    int endBitSubIndex = 64 - startBitSubIndex;
                                    val = (int) ((blockData[startIndex] >>> startBitSubIndex | blockData[endIndex] << endBitSubIndex) & maxEntryValue);
                                }

                                int id = 0;
                                byte data = 0;

                                CompoundTag blockTag = palette.get(val);
                                String name = blockTag.getStringValue("Name").get().substring(10); // Remove the namespace (minecraft: prefix)
                                DowngradeData.BlockEntry blockEntry = downgradeData.getBlocks().get(name);

                                if (blockEntry != null) {
                                    id = blockEntry.getId();
                                    data = (byte) blockEntry.getData();

                                    // Block properties
                                    Optional<CompoundTag> propertiesTag = blockTag.getAsCompoundTag("Properties");
                                    Map<String, String> properties = null;

                                    if (propertiesTag.isPresent()) {
                                        properties = propertiesTag.get().getValue().values().stream().map(Tag::getAsStringTag).filter(Optional::
                                                isPresent).map(Optional::get).collect(Collectors.toMap(Tag::getName, StringTag::getValue));

                                        if (blockEntry.getProperties() != null) {
                                            mainLoop:
                                            for (DowngradeData.BlockProperty property : blockEntry.getProperties()) {
                                                for (Map.Entry<String, String> conditionEntry : property.getConditions().entrySet()) {
                                                    String propertyName = conditionEntry.getKey();
                                                    String propertyValue = conditionEntry.getValue();
                                                    boolean inverted = propertyName.startsWith("!");

                                                    if ((inverted && propertyValue.equals(properties.get(propertyName.substring(1))))
                                                            || (!inverted && !propertyValue.equals(properties.get(propertyName)))) {
                                                        continue mainLoop;
                                                    }
                                                }

                                                // If we get to this point, all specified properties have the required value
                                                if (property.getId() != -1) {
                                                    id = property.getId();
                                                }

                                                if (property.getData() != -1) {
                                                    if (property.getOperation() == DowngradeData.Operation.OR) {
                                                        data |= property.getData();
                                                    } else {
                                                        data = (byte) property.getData();
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Tile Entity data
                                    DowngradeData.TileEntityData tileEntityData = blockEntry.getTileEntityData();

                                    if (tileEntityData != null) {
                                        CompoundTag tileEntityTag = null;

                                        // Search for existing tile entities
                                        int blockX = x + chunk.getX() * 16;
                                        int blockY = y + sectionIndex * 16;
                                        int blockZ = z + chunk.getZ() * 16;

                                        for (CompoundTag tileTag : chunk.getTileEntities()) {
                                            int tileX = tileTag.getIntValue("x").get();
                                            int tileY = tileTag.getIntValue("y").get();
                                            int tileZ = tileTag.getIntValue("z").get();

                                            if (tileX == blockX && tileY == blockY && tileZ == blockZ) {
                                                tileEntityTag = tileTag;

                                                break;
                                            }
                                        }

                                        // Create Tile Entity Action
                                        DowngradeData.TileCreateAction createAction = tileEntityData.getCreateAction();

                                        if (tileEntityTag == null) {
                                            if (createAction == null) {
                                                throw new IllegalStateException("No create action was specified for block " + name + " but no tile entity was found");
                                            }

                                            Objects.requireNonNull(createAction.getName(), "Tile entity type cannot be null (" + name + ")");

                                            CompoundMap tileMap = new CompoundMap();
                                            tileMap.put("id", new StringTag("id", "minecraft:" + createAction.getName()));
                                            tileMap.put("x", new IntTag("x", blockX));
                                            tileMap.put("y", new IntTag("y", blockY));
                                            tileMap.put("z", new IntTag("z", blockZ));
                                            tileEntityTag = new CompoundTag("", tileMap);
                                        }

                                        // Set Values Action
                                        DowngradeData.TileSetAction setAction = tileEntityData.getSetAction();

                                        if (setAction != null) {
                                            Map<String, DowngradeData.TileSetEntry> entries = setAction.getEntries();

                                            for (Map.Entry<String, DowngradeData.TileSetEntry> entry : entries.entrySet()) {
                                                String key = entry.getKey();
                                                DowngradeData.TileSetEntry value = entry.getValue();
                                                String nbtValue = value.getValue();

                                                // Retrieve value from property
                                                if (nbtValue.startsWith("@prop:")) {
                                                    if (properties == null) {
                                                        continue;
                                                    }

                                                    String propName = nbtValue.substring(6);
                                                    nbtValue = properties.get(propName);

                                                    if (nbtValue == null) {
                                                        throw new IllegalStateException("Block " + name + " doesn't have a property called " + propName);
                                                    }
                                                }

                                                Tag nbtTag;

                                                switch (value.getType().toLowerCase()) {
                                                    case "byte":
                                                        nbtTag = new ByteTag(key, Byte.valueOf(nbtValue));
                                                        break;
                                                    case "short":
                                                        nbtTag = new ShortTag(key, Short.valueOf(nbtValue));
                                                        break;
                                                    case "int":
                                                        nbtTag = new IntTag(key, Integer.valueOf(nbtValue));
                                                        break;
                                                    case "long":
                                                        nbtTag = new LongTag(key, Long.valueOf(nbtValue));
                                                        break;
                                                    case "float":
                                                        nbtTag = new FloatTag(key, Float.valueOf(nbtValue));
                                                        break;
                                                    case "double":
                                                        nbtTag = new DoubleTag(key, Double.valueOf(nbtValue));
                                                        break;
                                                    default:
                                                        nbtTag = new StringTag(key, nbtValue);
                                                        break;
                                                }

                                                tileEntityTag.getValue().put(key, nbtTag);
                                            }
                                        }
                                    }
                                }

                                blockArray[arrayIndex] = (byte) id;
                                dataArray.set(arrayIndex, data);
                            }
                        }
                    }

                    newSections[sectionIndex] = new CraftSlimeChunkSection(blockArray, dataArray, null, null, section.getBlockLight(), section.getSkyLight());
                }

                CompoundMap heightMap = new CompoundMap();
                heightMap.put("heightMap", new IntArrayTag("heightMap", new int[256]));

                SlimeChunk newChunk = new CraftSlimeChunk(world.getName(), chunk.getX(), chunk.getZ(), newSections, new CompoundTag("", heightMap),
                        new int[64], chunk.getTileEntities(), chunk.getEntities());
                world.updateChunk(newChunk);
            }

            int done = i + 1;
            if (done == chunks.size()) {
                Logging.info(ChatColor.GREEN + "World successfully converted to the 1.12 format!");
            } else if (System.currentTimeMillis() - lastMessage > 1000) {
                int percentage = (done * 100) / chunks.size();
                Logging.info("Converting world... " + percentage + "%");
                lastMessage = System.currentTimeMillis();
            }
        }
    }

    private void loadDowngradeData() throws IOException {
        Logging.info("Loading downgrade data...");
        GsonBuilder builder = new GsonBuilder();

        // Type Adapters
        builder.registerTypeAdapter(DowngradeData.class, new DowngradeDataDeserializer());
        builder.registerTypeAdapter(DowngradeData.BlockEntry.class, new BlockEntryDeserializer());
        builder.registerTypeAdapter(DowngradeData.TileSetAction.class, new SetActionDeserializer());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(SWMPlugin.getInstance().getResource("1_13to1_12_blocks.json"), StandardCharsets.UTF_8))) {
            downgradeData = builder.create().fromJson(reader, DowngradeData.class);
        }
    }
}
