package com.grinderwolf.swm.plugin.upgrade.v1_13.deserializers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.grinderwolf.swm.plugin.upgrade.v1_13.DowngradeData;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class BlockEntryDeserializer implements JsonDeserializer<DowngradeData.BlockEntry> {

    private static final Pattern PATTERN = Pattern.compile("\\|");

    @Override
    public DowngradeData.BlockEntry deserialize(JsonElement el, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = el.getAsJsonObject();
        Map<String[], DowngradeData.BlockProperty> properties;

        if (obj.has("properties")) {
            JsonObject propertiesObj = obj.getAsJsonObject("properties");
            properties = new LinkedHashMap<>();

            for (Map.Entry<String, JsonElement> entry : propertiesObj.entrySet()) {
                String propertyName = entry.getKey();
                JsonObject propertyData = entry.getValue().getAsJsonObject();

                properties.put(PATTERN.split(propertyName), context.deserialize(propertyData, DowngradeData.BlockProperty.class));
            }
        } else {
            properties = null;
        }

        int id = obj.has("id") ? obj.getAsJsonPrimitive("id").getAsInt() : 0;
        int data = obj.has("data") ? obj.getAsJsonPrimitive("data").getAsInt() : 0;

        DowngradeData.TileEntityData tileEntityData = context.deserialize(obj.getAsJsonObject("tile_entity"), DowngradeData.TileEntityData.class);
        return new DowngradeData.BlockEntry(id, data, properties, tileEntityData);
    }
}
