package com.grinderwolf.swm.plugin.config;

import com.google.common.reflect.TypeToken;
import com.grinderwolf.swm.plugin.log.Logging;
import lombok.Getter;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Getter
@ConfigSerializable
public class WorldsConfig {

    @Setting("worlds")
    private Map<String, WorldData> worlds = new HashMap<>();

    public void save() {
        try {
            ConfigManager.getWorldConfigLoader().save(ConfigManager.getWorldConfigLoader().createEmptyNode().setValue(TypeToken.of(WorldsConfig.class), this));
        } catch (IOException | ObjectMappingException ex) {
            Logging.error("Failed to save worlds config file:");
            ex.printStackTrace();
        }
    }
}
