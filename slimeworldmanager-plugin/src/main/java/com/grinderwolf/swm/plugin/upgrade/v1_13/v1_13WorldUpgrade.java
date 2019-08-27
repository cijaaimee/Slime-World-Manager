package com.grinderwolf.swm.plugin.upgrade.v1_13;

import com.flowpowered.nbt.ByteArrayTag;
import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
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
import com.grinderwolf.swm.plugin.upgrade.v1_13.deserializers.DowngradeDataDeserializer;
import com.grinderwolf.swm.plugin.upgrade.v1_13.deserializers.PropertyDeserializer;
import com.grinderwolf.swm.plugin.upgrade.v1_13.deserializers.PropertyValueDeserializer;
import com.grinderwolf.swm.plugin.upgrade.v1_13.deserializers.SetActionDeserializer;
import org.bukkit.ChatColor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class v1_13WorldUpgrade implements Upgrade {

    private DowngradeData downgradeData;

    @Override
    public void upgrade(CraftSlimeWorld world) {
        Logging.warning("Updating world to the 1.13 format. This may take a while.");

        List<SlimeChunk> chunks = new ArrayList<>(world.getChunks().values());
        chunks.sort(Comparator.comparingLong(chunk -> (long) chunk.getZ() * Integer.MAX_VALUE + (long) chunk.getX()));

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

            for (int index = 0; i < chunk.getBiomes().length; ++i) {
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

            for (SlimeChunkSection section : chunk.getSections()) {
                if (section != null) {
                    ListTag<CompoundTag> palette = section.getPalette();
                    long[] blockData = section.getBlockStates();

                    // TODO: Read all blocks from palette and convert them to the old system
                }
            }

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

    private void loadDowngradeData() throws IOException {
        Logging.info("Loading downgrade data...");
        GsonBuilder builder = new GsonBuilder();

        // Type Adapters
        builder.registerTypeAdapter(DowngradeData.class, new DowngradeDataDeserializer());
        builder.registerTypeAdapter(DowngradeData.BlockProperty.class, new PropertyDeserializer());
        builder.registerTypeAdapter(DowngradeData.BlockPropertyValue.class, new PropertyValueDeserializer());
        builder.registerTypeAdapter(DowngradeData.TileSetAction.class, new SetActionDeserializer());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(SWMPlugin.getInstance().getResource("1_13to1_12_blocks.json"), StandardCharsets.UTF_8))) {
            downgradeData = builder.create().fromJson(reader, DowngradeData.class);
        }
    }
}
