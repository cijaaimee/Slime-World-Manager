package com.grinderwolf.swm.nms.v1171;

import com.flowpowered.nbt.*;
import com.grinderwolf.swm.api.utils.*;
import com.grinderwolf.swm.api.world.*;
import com.grinderwolf.swm.nms.*;
import lombok.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.entity.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.lighting.*;

import java.util.*;

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
