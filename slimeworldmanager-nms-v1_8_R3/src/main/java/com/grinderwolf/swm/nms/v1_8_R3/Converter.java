/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.nms.v1_8_R3;

import com.flowpowered.nbt.*;
import com.grinderwolf.swm.api.utils.NibbleArray;
import net.minecraft.server.v1_8_R3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class Converter {

    private static final Logger LOGGER = LogManager.getLogger("SWM Converter");

    static net.minecraft.server.v1_8_R3.NibbleArray convertArray(NibbleArray array) {
        return new net.minecraft.server.v1_8_R3.NibbleArray(array.getBacking());
    }

    static NibbleArray convertArray(net.minecraft.server.v1_8_R3.NibbleArray array) {
        return new NibbleArray(array.a());
    }

    static NBTBase convertTag(Tag tag) {
        try {
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
                case TAG_INT_ARRAY:
                    return new NBTTagIntArray(((IntArrayTag) tag).getValue());
                case TAG_LIST:
                    NBTTagList list = new NBTTagList();
                    ((ListTag<?>) tag)
                            .getValue().stream().map(Converter::convertTag).forEach(list::add);

                    return list;
                case TAG_COMPOUND:
                    NBTTagCompound compound = new NBTTagCompound();

                    ((CompoundTag) tag)
                            .getValue()
                            .forEach((key, value) -> compound.set(key, convertTag(value)));

                    return compound;
                default:
                    throw new IllegalArgumentException("Invalid tag type " + tag.getType().name());
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to convert NBT object:");
            LOGGER.error(tag.toString());

            throw ex;
        }
    }

    static Tag convertTag(String name, NBTBase base) {
        switch (base.getTypeId()) {
            case 1:
                return new ByteTag(name, ((NBTTagByte) base).f());
            case 2:
                return new ShortTag(name, ((NBTBase.NBTNumber) base).e());
            case 3:
                return new IntTag(name, ((NBTBase.NBTNumber) base).d());
            case 4:
                return new LongTag(name, ((NBTBase.NBTNumber) base).c());
            case 5:
                return new FloatTag(name, ((NBTBase.NBTNumber) base).h());
            case 6:
                return new DoubleTag(name, ((NBTBase.NBTNumber) base).g());
            case 7:
                return new ByteArrayTag(name, ((NBTTagByteArray) base).c());
            case 8:
                return new StringTag(name, ((NBTTagString) base).a_());
            case 9:
                List<Tag> list = new ArrayList<>();
                NBTTagList originalList = ((NBTTagList) base);

                for (int i = 0; i < originalList.size(); i++) {
                    NBTBase entry = originalList.g(i);
                    list.add(convertTag("", entry));
                }

                return new ListTag<>(name, TagType.getById(originalList.f()), list);
            case 10:
                NBTTagCompound originalCompound = ((NBTTagCompound) base);
                CompoundTag compound = new CompoundTag(name, new CompoundMap());

                for (String key : originalCompound.c()) {
                    compound.getValue().put(key, convertTag(key, originalCompound.get(key)));
                }

                return compound;
            case 11:
                return new IntArrayTag("", ((NBTTagIntArray) base).c());
            default:
                throw new IllegalArgumentException("Invalid tag type " + base.getTypeId());
        }
    }
}
