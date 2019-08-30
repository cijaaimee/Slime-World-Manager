package com.grinderwolf.swm.plugin.upgrade.v1_13.deserializers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.grinderwolf.swm.plugin.upgrade.v1_13.DowngradeData;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class DowngradeDataDeserializer implements JsonDeserializer<DowngradeData> {

    @Override
    public DowngradeData deserialize(JsonElement el, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = el.getAsJsonObject();
        Map<String, DowngradeData.BlockEntry> blocks = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement blockEl = entry.getValue();

            blocks.put(key, context.deserialize(blockEl, DowngradeData.BlockEntry.class));
        }

        return new DowngradeData(blocks);
    }
}
