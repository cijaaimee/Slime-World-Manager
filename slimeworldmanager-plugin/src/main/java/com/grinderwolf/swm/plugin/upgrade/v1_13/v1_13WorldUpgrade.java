package com.grinderwolf.swm.plugin.upgrade.v1_13;

import com.flowpowered.nbt.ByteArrayTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.TagType;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.upgrade.Upgrade;

import java.util.ArrayList;

public class v1_13WorldUpgrade implements Upgrade {

    @Override
    public void upgrade(CraftSlimeWorld world) {
        for (SlimeChunk chunk : world.getChunks().values()) {
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

            globalTag.getValue().put("Data", chunkTag);

            CompoundTag newGlobalTag = SWMPlugin.getInstance().getNms().convertChunk(globalTag);

            // TODO update the values of the SlimeChunk
        }
    }

    @Override
    public void downgrade(CraftSlimeWorld world) {

    }

    private static ListTag<CompoundTag> serializeSections(SlimeChunkSection[] sections) {
        ListTag<CompoundTag> sectionList = new ListTag<CompoundTag>("Sections", TagType.TAG_COMPOUND, new ArrayList<>());

        for (int i = 0; i < sections.length; i++) {
            SlimeChunkSection section = sections[i];

            if (section != null) {
                CompoundTag sectionTag = new CompoundTag(i + "", new CompoundMap());

                sectionTag.getValue().put("Y", new IntTag("Y", i));
                sectionTag.getValue().put("Blocks", new ByteArrayTag("Blocks", section.getBlocks()));
                sectionTag.getValue().put("Data", new ByteArrayTag("Data", section.getData().getBacking()));
                sectionList.getValue().add(sectionTag);
            }
        }

        return sectionList;
    }
}
