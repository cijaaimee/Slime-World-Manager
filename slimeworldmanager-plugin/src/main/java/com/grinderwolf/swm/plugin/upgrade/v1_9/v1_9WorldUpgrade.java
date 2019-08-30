package com.grinderwolf.swm.plugin.upgrade.v1_9;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.StringTag;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.grinderwolf.swm.plugin.upgrade.Upgrade;

public class v1_9WorldUpgrade implements Upgrade {

    private static final JsonParser PARSER = new JsonParser();

    @Override
    public void upgrade(CraftSlimeWorld world) {
        // In 1.9, all signs must be formatted using JSON
        for (SlimeChunk chunk : world.getChunks().values()) {
            for (CompoundTag entityTag : chunk.getTileEntities()) {
                String type = entityTag.getAsStringTag("id").get().getValue();

                if (type.equals("Sign")) {
                    CompoundMap map = entityTag.getValue();

                    for (int i = 1; i < 5; i++) {
                        String id = "Text" + i;

                        map.put(id, new StringTag(id, fixJson(entityTag.getAsStringTag(id).map(StringTag::getValue).orElse(null))));
                    }
                }
            }
        }
    }

    @Override
    public void downgrade(CraftSlimeWorld world) {
        // No need to downgrade as JSON signs are compatible with 1.8
    }

    private static String fixJson(String value) {
        if (value == null || value.equalsIgnoreCase("null") || value.isEmpty()) {
            return "{\"text\":\"\"}";
        }

        try {
            PARSER.parse(value);
        } catch (JsonSyntaxException ex) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("text", value);

            return jsonObject.toString();
        }

        return value;
    }
}
