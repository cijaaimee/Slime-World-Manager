package com.grinderwolf.swm.api.world.properties;

import com.flowpowered.nbt.*;
import lombok.*;

import java.util.*;

/**
 * A Property Map object.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SlimePropertyMap {

    @Getter(value = AccessLevel.PRIVATE)
    private final Map<SlimeProperty, Object> values;

    public SlimePropertyMap() {
        this(new HashMap<>());
    }

    /**
     * Returns the string value of a given property.
     *
     * @param property The property to retrieve the value of.
     * @return the {@link String} value of the property or the default value if unset.
     * @throws IllegalArgumentException if the property type is not {@link PropertyType#STRING}.
     */
    public String getString(SlimeProperty property) {
        ensureType(property, PropertyType.STRING);
        String value = (String) values.get(property);

        if (value == null) {
            value = (String) property.getDefaultValue();
        }

        return value;
    }

    /**
     * Updates a property with a given string value.
     *
     * @param property The property to update.
     * @param value The value to set.
     * @throws IllegalArgumentException if the property type is not {@link PropertyType#STRING}.
     */
    public void setString(SlimeProperty property, String value) {
        Objects.requireNonNull(value, "Property value cannot be null");
        ensureType(property, PropertyType.STRING);

        if (property.getValidator() != null && !property.getValidator().apply(value)) {
            throw new IllegalArgumentException("'" + value + "' is not a valid property value.");
        }

        values.put(property, value);
    }

    /**
     * Returns the boolean value of a given property.
     *
     * @param property The property to retrieve the value of.
     * @return the {@link Boolean} value of the property or the default value if unset.
     * @throws IllegalArgumentException if the property type is not {@link PropertyType#BOOLEAN}.
     */
    public Boolean getBoolean(SlimeProperty property) {
        ensureType(property, PropertyType.BOOLEAN);
        Boolean value = (Boolean) values.get(property);

        if (value == null) {
            value = (Boolean) property.getDefaultValue();
        }

        return value;
    }

    /**
     * Updates a property with a given boolean value.
     *
     * @param property The property to update.
     * @param value The value to set.
     * @throws IllegalArgumentException if the property type is not {@link PropertyType#BOOLEAN}.
     */
    public void setBoolean(SlimeProperty property, boolean value) {
        ensureType(property, PropertyType.BOOLEAN);
        // There's no need to validate the value, why'd you ever have a validator for a boolean?
        values.put(property, value);
    }

    /**
     * Returns the int value of a given property.
     *
     * @param property The property to retrieve the value of.
     * @return the int value of the property or the default value if unset.
     * @throws IllegalArgumentException if the property type is not {@link PropertyType#INT}.
     */
    public int getInt(SlimeProperty property) {
        ensureType(property, PropertyType.INT);
        Integer value = (Integer) values.get(property);

        if (value == null) {
            value = (Integer) property.getDefaultValue();
        }

        return value;
    }

    /**
     * Updates a property with a given int value.
     *
     * @param property The property to update.
     * @param value The value to set.
     * @throws IllegalArgumentException if the property type is not {@link PropertyType#INT}.
     */
    public void setInt(SlimeProperty property, int value) {
        ensureType(property, PropertyType.INT);

        if (property.getValidator() != null && !property.getValidator().apply(value)) {
            throw new IllegalArgumentException("'" + value + "' is not a valid property value.");
        }

        values.put(property, value);
    }

    private void ensureType(SlimeProperty property, PropertyType requiredType) {
        if (property.getType() != requiredType) {
            throw new IllegalArgumentException("Property " + property.getNbtName() + " type is " + property.getType().name() + ", not " + requiredType.name());
        }
    }

    /**
     * Copies all values from the specified {@link SlimePropertyMap}.
     * If the same property has different values on both maps, the one
     * on the providen map will be used.
     *
     * @param propertyMap A {@link SlimePropertyMap}.
     */
    public void merge(SlimePropertyMap propertyMap) {
        values.putAll(propertyMap.getValues());
    }

    /**
     * Returns a {@link CompoundTag} containing every property set in this map.
     *
     * @return A {@link CompoundTag} with all the properties stored in this map.
     */
    public CompoundTag toCompound() {
        CompoundMap map = new CompoundMap();

        for (Map.Entry<SlimeProperty, Object> entry : values.entrySet()) {
            SlimeProperty property = entry.getKey();
            Object value = entry.getValue();

            switch (property.getType()) {
                case STRING:
                    map.put(property.getNbtName(), new StringTag(property.getNbtName(), (String) value));
                    break;
                case BOOLEAN:
                    map.put(property.getNbtName(), new ByteTag(property.getNbtName(), (byte) (((Boolean) value) ? 1 : 0)));
                    break;
                case INT:
                    map.put(property.getNbtName(), new IntTag(property.getNbtName(), (Integer) value));
                    break;
            }
        }

        return new CompoundTag("properties", map);
    }

    /**
     * Creates a {@link SlimePropertyMap} based off a {@link CompoundTag}.
     *
     * @param compound A {@link CompoundTag} to get the properties from.
     * @return A {@link SlimePropertyMap} with the properties from the provided {@link CompoundTag}.
     */
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
