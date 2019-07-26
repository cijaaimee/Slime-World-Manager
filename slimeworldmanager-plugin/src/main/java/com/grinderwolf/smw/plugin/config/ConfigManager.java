package com.grinderwolf.smw.plugin.config;

import com.grinderwolf.smw.plugin.SMWPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ConfigManager {

    private static final File PLUGIN_DIR = new File("plugins", "SlimeWorldManager");

    public static FileConfiguration getFile(String name) throws IOException {
        PLUGIN_DIR.mkdirs();

        File file = new File(PLUGIN_DIR, name + ".yml");

        if (!file.exists()) {
            Files.copy(SMWPlugin.getInstance().getResource(name + ".yml"), file.toPath());
        }

        return YamlConfiguration.loadConfiguration(file);
    }
}
