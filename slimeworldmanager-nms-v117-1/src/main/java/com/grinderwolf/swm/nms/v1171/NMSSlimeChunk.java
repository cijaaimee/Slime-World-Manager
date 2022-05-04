package com.grinderwolf.swm.nms.v1171;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.LongArrayTag;
import com.grinderwolf.swm.api.utils.NibbleArray;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.nms.CraftSlimeChunkSection;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class NMSSlimeChunk implements SlimeChunk {

    private LevelChunk chunk;

    @Override
    public String getWorldName() {
        return chunk.getLevel().getMinecraftWorld().serverLevelData.getLevelName();
    }

    @Override
    public int getX() {
        return chunk.getPos().x;
    }

    @Override
    public int getZ() {
        return chunk.getPos().z;
    }

    @Override
    public SlimeChunkSection[] getSections() {
        SlimeChunkSection[] sections = new SlimeChunkSection[16]; // todo no static number of sections, see 1.18
        LevelLightEngine lightEngine = chunk.getLevel().getChunkSource().getLightEngine();

        // todo 1.17 can have negative sections
        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            LevelChunkSection section = chunk.getSections()[sectionId];

            if (section != null && !section.isEmpty()) {
                // Block Light Nibble Array
                NibbleArray blockLightArray = Converter.convertArray(lightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunk.getPos(), sectionId)));

                // Sky light Nibble Array
                NibbleArray skyLightArray = Converter.convertArray(lightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunk.getPos(), sectionId)));

                // Tile/Entity Data

                // Block Data
                PalettedContainer<BlockState> dataPaletteBlock = section.states;
                net.minecraft.nbt.CompoundTag blocksCompound = new net.minecraft.nbt.CompoundTag();
                dataPaletteBlock.write(blocksCompound, "Palette", "BlockStates");
                net.minecraft.nbt.ListTag paletteList = blocksCompound.getList("Palette", 10);
                ListTag<CompoundTag> palette = (ListTag<CompoundTag>) Converter.convertTag("", paletteList);
                long[] blockStates = blocksCompound.getLongArray("BlockStates");

                sections[sectionId] = new CraftSlimeChunkSection(palette, blockStates, null, null, blockLightArray, skyLightArray);
            }
        }

        return sections;
    }

    @Override
    public int getMinSection() {
        return this.chunk.getMinSection();
    }

    @Override
    public int getMaxSection() {
        return this.chunk.getMaxSection();
    }

    @Override
    public CompoundTag getHeightMaps() {
        // HeightMap
        CompoundMap heightMaps = new CompoundMap();

        for (Map.Entry<Heightmap.Types, Heightmap> entry : chunk.heightmaps.entrySet()) {
            Heightmap.Types type = entry.getKey();
            Heightmap map = entry.getValue();

            heightMaps.put(type.name(), new LongArrayTag(type.name(), map.getRawData()));
        }

        return new CompoundTag("", heightMaps);
    }

    @Override
    public int[] getBiomes() {
        return chunk.getBiomes().writeBiomes();
    }

    @Override
    public List<CompoundTag> getTileEntities() {
        List<CompoundTag> tileEntities = new ArrayList<>();

        for (BlockEntity entity : chunk.blockEntities.values()) {
            net.minecraft.nbt.CompoundTag entityNbt = new net.minecraft.nbt.CompoundTag();
            entity.save(entityNbt);
            tileEntities.add((CompoundTag) Converter.convertTag(entityNbt.getString("name"), entityNbt));
        }

        return tileEntities;
    }

    @Override
    public List<CompoundTag> getEntities() {
        List<CompoundTag> entities = new ArrayList<>();

        PersistentEntitySectionManager<Entity> entityManager = chunk.level.entityManager;

        for (Entity entity : entityManager.getEntityGetter().getAll()) {
            ChunkPos chunkPos = chunk.getPos();
            ChunkPos entityPos = entity.chunkPosition();

            if (chunkPos.x == entityPos.x && chunkPos.z == entityPos.z) {
                net.minecraft.nbt.CompoundTag entityNbt = new net.minecraft.nbt.CompoundTag();
                if (entity.save(entityNbt)) {
                    entities.add((CompoundTag) Converter.convertTag("", entityNbt));
                }
            }
        }

        return entities;
    }
}
