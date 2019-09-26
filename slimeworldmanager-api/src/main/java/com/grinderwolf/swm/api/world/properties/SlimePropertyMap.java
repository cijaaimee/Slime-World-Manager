package com.grinderwolf.swm.api.world.properties;

import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.StringTag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SlimePropertyMap {

    private final Map<SlimeProperty, Object> values;

    public SlimePropertyMap() {
        this(new HashMap<>());
    }

    public String getString(SlimeProperty property) {
        ensureType(property, PropertyType.STRING);
        String value = (String) values.get(property);

        if (value == null) {
            value = (String) property.getDefaultValue();
        }

        return value;
    }

    public void setString(SlimeProperty property, String value) {
        ensureType(property, PropertyType.STRING);

        if (Objects.equals(property.getDefaultValue(), value)) {
            values.remove(property);
        } else {
            values.put(property, value);
        }
    }

    public Boolean getBoolean(SlimeProperty property) {
        ensureType(property, PropertyType.BOOLEAN);
        Boolean value = (Boolean) values.get(property);

        if (value == null) {
            value = (Boolean) property.getDefaultValue();
        }

        return value;
    }

    public void setBoolean(SlimeProperty property, Boolean value) {
        ensureType(property, PropertyType.BOOLEAN);

        if (Objects.equals(property.getDefaultValue(), value)) {
            values.remove(property);
        } else {
            values.put(property, value);
        }
    }

    public Integer getInt(SlimeProperty property) {
        ensureType(property, PropertyType.INT);
        Integer value = (Integer) values.get(property);

        if (value == null) {
            value = (Integer) property.getDefaultValue();
        }

        return value;
    }

    public void setInt(SlimeProperty property, Integer value) {
        ensureType(property, PropertyType.INT);

        if (Objects.equals(property.getDefaultValue(), value)) {
            values.remove(property);
        } else {
            values.put(property, value);
        }
    }

    private void ensureType(SlimeProperty property, PropertyType requiredType) {
        if (property.getType() != requiredType) {
            throw new IllegalArgumentException("Property " + property.getNbtName() + " type is " + property.getType().name() + ", not " + requiredType.name());
        }
    }

    public CompoundTag toCompound() {
        CompoundMap map = new CompoundMap();

        for (Map.Entry<SlimeProperty, Object> entry : values.entrySet()) {
            SlimeProperty property = entry.getKey();
            Object value = entry.getValue();

            switch (property.getType()) {
                case STRING:
                    map.put(property.getNbtName(), new StringTag(property.getNbtName(), (String) value));
                case BOOLEAN:
                    map.put(property.getNbtName(), new ByteTag(property.getNbtName(), (byte) (((Boolean) value) ? 1 : 0)));
                case INT:
                    map.put(property.getNbtName(), new IntTag(property.getNbtName(), (byte) (((Boolean) value) ? 1 : 0)));
            }
        }

        return new CompoundTag("properties", map);
    }

    public static SlimePropertyMap fromCompound(CompoundTag compound) {
        Map<SlimeProperty, Object> values = new HashMap<>();

        for (SlimeProperty property : SlimeProperties.VALUES) {
            switch (property.getType()) {
                case STRING:
                    compound.getStringValue(property.getNbtName()).ifPresent((value) -> values.put(property, value));
                    break;
                case BOOLEAN:
                    compound.getByteValue(property.getNbtName()).map((value) -> value == 1).ifPresent((value) -> values.put(property, value));
                    break;
                case INT:
                    compound.getIntValue(property.getNbtName()).ifPresent((value) -> values.put(property, value));
                    break;
            }
        }

        return new SlimePropertyMap(values);
    }
}
