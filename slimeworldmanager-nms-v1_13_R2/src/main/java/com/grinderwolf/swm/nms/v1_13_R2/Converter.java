package com.grinderwolf.swm.nms.v1_13_R2;

import com.flowpowered.nbt.ByteArrayTag;
import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.DoubleTag;
import com.flowpowered.nbt.FloatTag;
import com.flowpowered.nbt.IntArrayTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.LongArrayTag;
import com.flowpowered.nbt.LongTag;
import com.flowpowered.nbt.ShortTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.TagType;
import com.grinderwolf.swm.api.utils.NibbleArray;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.nms.CraftSlimeChunk;
import com.grinderwolf.swm.nms.CraftSlimeChunkSection;
import net.minecraft.server.v1_13_R2.BiomeBase;
import net.minecraft.server.v1_13_R2.Chunk;
import net.minecraft.server.v1_13_R2.ChunkSection;
import net.minecraft.server.v1_13_R2.DataPaletteBlock;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.HeightMap;
import net.minecraft.server.v1_13_R2.IRegistry;
import net.minecraft.server.v1_13_R2.NBTBase;
import net.minecraft.server.v1_13_R2.NBTTagByte;
import net.minecraft.server.v1_13_R2.NBTTagByteArray;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.NBTTagDouble;
import net.minecraft.server.v1_13_R2.NBTTagFloat;
import net.minecraft.server.v1_13_R2.NBTTagInt;
import net.minecraft.server.v1_13_R2.NBTTagIntArray;
import net.minecraft.server.v1_13_R2.NBTTagList;
import net.minecraft.server.v1_13_R2.NBTTagLong;
import net.minecraft.server.v1_13_R2.NBTTagLongArray;
import net.minecraft.server.v1_13_R2.NBTTagShort;
import net.minecraft.server.v1_13_R2.NBTTagString;
import net.minecraft.server.v1_13_R2.TileEntity;

import java.util.ArrayList;
import java.util.List;

public class Converter {

    public static net.minecraft.server.v1_13_R2.NibbleArray convertArray(NibbleArray array) {
        return new net.minecraft.server.v1_13_R2.NibbleArray(array.getBacking());
    }

    public static NibbleArray convertArray(net.minecraft.server.v1_13_R2.NibbleArray array) {
        return new NibbleArray(array.asBytes());
    }

    public static NBTBase convertTag(Tag tag) {
        switch (tag.getType()) {
            case TAG_BYTE:
                return new NBTTagByte(((ByteTag) tag).getValue());
            case TAG_SHORT:
                return new NBTTagShort(((ShortTag) tag).getValue());
            case TAG_INT:
                return new NBTTagInt(((IntTag) tag).getValue());
            case TAG_LONG:
                return new NBTTagLong(((LongTag) tag).getValue());
            case TAG_FLOAT:
                return new NBTTagFloat(((FloatTag) tag).getValue());
            case TAG_DOUBLE:
                return new NBTTagDouble(((DoubleTag) tag).getValue());
            case TAG_BYTE_ARRAY:
                return new NBTTagByteArray(((ByteArrayTag) tag).getValue());
            case TAG_STRING:
                return new NBTTagString(((StringTag) tag).getValue());
            case TAG_LIST:
                NBTTagList list = new NBTTagList();

                //noinspection unchecked
                ((ListTag<?>) tag).getValue().stream().map(Converter::convertTag).forEach(list::add);

                return list;
            case TAG_COMPOUND:
                NBTTagCompound compound = new NBTTagCompound();

                ((CompoundTag) tag).getValue().forEach((key, value) -> compound.set(key, convertTag(value)));

                return compound;
            case TAG_INT_ARRAY:
                return new NBTTagIntArray(((IntArrayTag) tag).getValue());
            case TAG_LONG_ARRAY:
                return new NBTTagLongArray(((LongArrayTag) tag).getValue());
            default:
                throw new IllegalArgumentException("Invalid tag type " + tag.getType().name());
        }
    }

    private static Tag convertTag(String name, NBTBase base) {
        switch (base.getTypeId()) {
            case 1:
                return new ByteTag(name, ((NBTTagByte) base).asByte());
            case 2:
                return new ShortTag(name, ((NBTTagShort) base).asShort());
            case 3:
                return new IntTag(name, ((NBTTagInt) base).asInt());
            case 4:
                return new LongTag(name, ((NBTTagLong) base).asLong());
            case 5:
                return new FloatTag(name, ((NBTTagFloat) base).asFloat());
            case 6:
                return new DoubleTag(name, ((NBTTagDouble) base).asDouble());
            case 7:
                return new ByteArrayTag(name, ((NBTTagByteArray) base).c());
            case 8:
                return new StringTag(name, ((NBTTagString) base).asString());
            case 9:
                List<Tag> list = new ArrayList<>();
                NBTTagList originalList = ((NBTTagList) base);

                for (int i = 0; i < originalList.size(); i++) {
                    NBTBase entry = originalList.get(i);
                    list.add(convertTag("", entry));
                }

                return new ListTag(name, TagType.getById(originalList.d()), list);
            case 10:
                NBTTagCompound originalCompound = ((NBTTagCompound) base);
                CompoundTag compound = new CompoundTag(name, new CompoundMap());

                for (String key : originalCompound.getKeys()) {
                    compound.getValue().put(key, convertTag(key, originalCompound.get(key)));
                }

                return compound;
            case 11:
                return new IntArrayTag("", ((NBTTagIntArray) base).d());
            case 12:
                return new LongArrayTag("", ((NBTTagLongArray) base).d());
            default:
                throw new IllegalArgumentException("Invalid tag type " + base.getTypeId());
        }
    }

    public static SlimeChunk convertChunk(Chunk chunk) {
        // Chunk sections
        SlimeChunkSection[] sections = new SlimeChunkSection[16];

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            ChunkSection section = chunk.getSections()[sectionId];

            if (section != null) {
                section.recalcBlockCounts();

                if (!section.a()) { // If the section is empty, just ignore it to save space
                    // Block Light Nibble Array
                    NibbleArray blockLightArray = convertArray(section.getEmittedLightArray());

                    // Sky light Nibble Array
                    NibbleArray skyLightArray = convertArray(section.getSkyLightArray());

                    // Block Data
                    DataPaletteBlock dataPaletteBlock = section.getBlocks();
                    NBTTagCompound blocksCompound = new NBTTagCompound();
                    dataPaletteBlock.b(blocksCompound, "Palette", "BlockStates");
                    NBTTagList paletteList = blocksCompound.getList("Palette", 10);
                    ListTag<CompoundTag> palette = (ListTag<CompoundTag>) Converter.convertTag("", paletteList);
                    long[] blockStates = blocksCompound.o("BlockStates");

                    sections[sectionId] = new CraftSlimeChunkSection(null, null, palette, blockStates, blockLightArray, skyLightArray);
                }
            }
        }

        // Tile Entities
        List<CompoundTag> tileEntities = new ArrayList<>();

        for (TileEntity entity : chunk.getTileEntities().values()) {
            NBTTagCompound entityNbt = new NBTTagCompound();
            entity.save(entityNbt);
            tileEntities.add((CompoundTag) convertTag("", entityNbt));
        }

        // Entities
        List<CompoundTag> entities = new ArrayList<>();

        for (int i = 0; i < chunk.getEntitySlices().length; i++) {
            for (Entity entity : chunk.getEntitySlices()[i]) {
                NBTTagCompound entityNbt = new NBTTagCompound();

                if (entity.d(entityNbt)) {
                    chunk.f(true);
                    entities.add((CompoundTag) convertTag("", entityNbt));
                }
            }
        }

        // Biomes
        BiomeBase[] biomeBases = chunk.getBiomeIndex();
        int[] biomes = new int[biomeBases.length];

        for (int i = 0; i < biomeBases.length; i++) {
            biomes[i] = IRegistry.BIOME.a(biomeBases[i]);
        }

        // HeightMap
        CompoundMap heightMaps = new CompoundMap();

        for (HeightMap.Type type : chunk.heightMap.keySet()) {
            heightMaps.put(type.b(), new LongArrayTag(type.b(), chunk.b(type).b()));
        }

        CompoundTag heightMapCompound = new CompoundTag("", heightMaps);

        return new CraftSlimeChunk(chunk.world.worldData.getName(), chunk.locX, chunk.locZ, sections, heightMapCompound, biomes, tileEntities, entities);
    }
}
