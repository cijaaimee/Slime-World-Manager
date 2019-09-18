package com.grinderwolf.swm.plugin.config;

import lombok.Data;
import lombok.Getter;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@Data
@ConfigSerializable
public class MainConfig {

    @Setting(value = "enable_async_world_gen", comment = "Only enable this if you don't have any other plugins that generate worlds.")
    private boolean asyncWorldGenerate = false;

    @Setting("updater")
    private UpdaterOptions updaterOptions = new UpdaterOptions();

    @Getter
    @ConfigSerializable
    public static class UpdaterOptions {

        @Setting(value = "enabled")
        private boolean enabled = true;

        @Setting(value = "onjoinmessage")
        private boolean messageEnabled = true;
    }
}
