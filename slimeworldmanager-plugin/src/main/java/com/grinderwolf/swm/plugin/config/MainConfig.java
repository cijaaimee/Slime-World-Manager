package com.grinderwolf.swm.plugin.config;

import lombok.Getter;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@Getter
@ConfigSerializable
public class MainConfig {

    @Setting(value = "enable_async_world_gen", comment = "Only enable this if you don't have any other plugins that generate worlds.")
    private boolean asyncWorldGenerate = false;
}