package com.grinderwolf.swm.plugin.upgrade.v1_13.deserializers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.grinderwolf.swm.plugin.upgrade.v1_13.DowngradeData;

import java.lang.reflect.Type;

public class PropertyValueDeserializer implements JsonDeserializer<DowngradeData.BlockPropertyValue> {

    @Override
    public DowngradeData.BlockPropertyValue deserialize(JsonElement el, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = el.getAsJsonObject();
        int id = -1;

        if (obj.has("id")) {
            id = obj.getAsJsonPrimitive("id").getAsInt();
        }

        int data = -1;
        DowngradeData.PropertyOperation operation = DowngradeData.PropertyOperation.REPLACE;

        if (obj.has("data")) {
            JsonPrimitive jsonData = obj.getAsJsonPrimitive("data");

            if (jsonData.isNumber()) {
                data = jsonData.getAsInt();
            } else {
                String opData = jsonData.getAsString();
                String opString = opData.substring(0, 1);

                if ("|".equals(opString)) {
                    operation = DowngradeData.PropertyOperation.OR;
                }

                data = Integer.parseInt(opData.substring(1));
            }
        }

        return new DowngradeData.BlockPropertyValue(id, data, operation);
    }
}
