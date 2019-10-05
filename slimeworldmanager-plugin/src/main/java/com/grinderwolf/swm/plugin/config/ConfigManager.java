package com.grinderwolf.swm.plugin.config;

import com.google.common.reflect.TypeToken;
import com.grinderwolf.swm.plugin.SWMPlugin;
import lombok.AccessLevel;
import lombok.Getter;
import ninja.leaping.configurate.loader.HeaderMode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.yaml.snakeyaml.DumperOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ConfigManager {

    private static final File PLUGIN_DIR = new File("plugins", "SlimeWorldManager");
    private static final File MAIN_FILE = new File(PLUGIN_DIR, "main.yml");
    private static final File WORLDS_FILE = new File(PLUGIN_DIR, "worlds.yml");
    private static final File SOURCES_FILE = new File(PLUGIN_DIR, "sources.yml");

    @Getter
    private static MainConfig mainConfig;
    @Getter(value = AccessLevel.PACKAGE)
    private static YAMLConfigurationLoader mainConfigLoader;

    @Getter
    private static WorldsConfig worldConfig;
    @Getter(value = AccessLevel.PACKAGE)
    private static YAMLConfigurationLoader worldConfigLoader;

    @Getter
    private static DatasourcesConfig datasourcesConfig;

    public static void initialize() throws IOException, ObjectMappingException {
        copyDefaultConfigs();

        mainConfigLoader = YAMLConfigurationLoader.builder().setPath(MAIN_FILE.toPath())
                .setFlowStyle(DumperOptions.FlowStyle.BLOCK).setHeaderMode(HeaderMode.PRESERVE).build();
        mainConfig = mainConfigLoader.load().getValue(TypeToken.of(MainConfig.class));

        worldConfigLoader = YAMLConfigurationLoader.builder().setPath(WORLDS_FILE.toPath())
                .setFlowStyle(DumperOptions.FlowStyle.BLOCK).setHeaderMode(HeaderMode.PRESERVE).build();
        worldConfig = worldConfigLoader.load().getValue(TypeToken.of(WorldsConfig.class));

        YAMLConfigurationLoader datasourcesConfigLoader = YAMLConfigurationLoader.builder().setPath(SOURCES_FILE.toPath())
                .setFlowStyle(DumperOptions.FlowStyle.BLOCK).setHeaderMode(HeaderMode.PRESERVE).build();
        datasourcesConfig = datasourcesConfigLoader.load().getValue(TypeToken.of(DatasourcesConfig.class));

        mainConfigLoader.save(mainConfigLoader.createEmptyNode().setValue(TypeToken.of(MainConfig.class), mainConfig));
        worldConfig.save();
        datasourcesConfigLoader.save(datasourcesConfigLoader.createEmptyNode().setValue(TypeToken.of(DatasourcesConfig.class), datasourcesConfig));
    }

    private static void copyDefaultConfigs() throws IOException {
        PLUGIN_DIR.mkdirs();

        if (!MAIN_FILE.exists()) {
            Files.copy(SWMPlugin.getInstance().getResource("main.yml"), MAIN_FILE.toPath());
        }

        if (!WORLDS_FILE.exists()) {
            Files.copy(SWMPlugin.getInstance().getResource("worlds.yml"), WORLDS_FILE.toPath());
        }

        if (!SOURCES_FILE.exists()) {
            Files.copy(SWMPlugin.getInstance().getResource("worlds.yml"), SOURCES_FILE.toPath());
        }
    }
}
