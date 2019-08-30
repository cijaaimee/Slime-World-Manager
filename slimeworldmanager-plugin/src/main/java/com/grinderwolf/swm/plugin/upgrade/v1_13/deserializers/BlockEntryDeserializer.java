package com.grinderwolf.swm.plugin.upgrade.v1_13.deserializers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.grinderwolf.swm.plugin.upgrade.v1_13.DowngradeData;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockEntryDeserializer implements JsonDeserializer<DowngradeData.BlockEntry> {

    private static final Pattern PATTERN = Pattern.compile("([!A-Za-z1-9]+)=([A-Za-z1-9]+)");

    @Override
    public DowngradeData.BlockEntry deserialize(JsonElement el, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = el.getAsJsonObject();
        List<DowngradeData.BlockProperty> properties;

        if (obj.has("properties")) {
            JsonObject propertiesObj = obj.getAsJsonObject("properties");
            properties = new ArrayList<>();

            for (Map.Entry<String, JsonElement> entry : propertiesObj.entrySet()) {
                String conditionsString = entry.getKey();

                Map<String, String> conditions = new HashMap<>();
                Matcher matcher = PATTERN.matcher(conditionsString);

                while (matcher.find()) {
                    String property = matcher.group(1);
                    String value = matcher.group(2);

                    conditions.put(property, value);
                }

                JsonObject propertyObj = entry.getValue().getAsJsonObject();
                int id = -1;

                if (propertyObj.has("id")) {
                    id = propertyObj.getAsJsonPrimitive("id").getAsInt();
                }

                int data = -1;
                DowngradeData.Operation operation = DowngradeData.Operation.REPLACE;

                if (propertyObj.has("data")) {
                    JsonPrimitive jsonData = propertyObj.getAsJsonPrimitive("data");

                    if (jsonData.isNumber()) {
                        data = jsonData.getAsInt();
                    } else {
                        String opData = jsonData.getAsString();
                        String opString = opData.substring(0, 1);

                        if ("|".equals(opString)) {
                            operation = DowngradeData.Operation.OR;
                        }

                        data = Integer.parseInt(opData.substring(1));
                    }
                }

                properties.add(new DowngradeData.BlockProperty(conditions, id, data, operation));
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
