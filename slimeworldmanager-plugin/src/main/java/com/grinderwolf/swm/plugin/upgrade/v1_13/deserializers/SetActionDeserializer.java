/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.plugin.upgrade.v1_13.deserializers;

import com.google.gson.*;
import com.grinderwolf.swm.plugin.upgrade.v1_13.DowngradeData;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class SetActionDeserializer implements JsonDeserializer<DowngradeData.TileSetAction> {

    @Override
    public DowngradeData.TileSetAction deserialize(
            JsonElement el, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject obj = el.getAsJsonObject();
        Map<String, DowngradeData.TileSetEntry> entries = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement entryEl = entry.getValue();

            entries.put(key, context.deserialize(entryEl, DowngradeData.TileSetEntry.class));
        }

        return new DowngradeData.TileSetAction(entries);
    }
}
