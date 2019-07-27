package com.grinderwolf.smw.plugin;

import com.grinderwolf.smw.api.SlimePlugin;
import com.grinderwolf.smw.api.exceptions.CorruptedWorldException;
import com.grinderwolf.smw.api.exceptions.InvalidVersionException;
import com.grinderwolf.smw.api.exceptions.NewerFormatException;
import com.grinderwolf.smw.api.exceptions.UnknownWorldException;
import com.grinderwolf.smw.api.loaders.SlimeLoader;
import com.grinderwolf.smw.api.loaders.SlimeLoaders;
import com.grinderwolf.smw.api.world.SlimeWorld;
import com.grinderwolf.smw.nms.SlimeNMS;
import com.grinderwolf.smw.nms.v1_8_R3.v1_8_R3SlimeNMS;
import com.grinderwolf.smw.plugin.commands.CommandManager;
import com.grinderwolf.smw.plugin.config.ConfigManager;
import com.grinderwolf.smw.plugin.loaders.LoaderUtils;
import com.grinderwolf.smw.plugin.log.Logging;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class SMWPlugin extends JavaPlugin implements SlimePlugin {

    @Getter
    private static SMWPlugin instance;
    @Getter
    private SlimeNMS nms;

    @Override
    public void onLoad() {
        Logging.info("Loading...");
        instance = this;

        LoaderUtils.registerLoaders();

        try {
            nms = loadInjector();
        } catch (InvalidVersionException ex) {
            Logging.error("Couldn't load injector:");
            ex.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        if (nms == null) {
            this.setEnabled(false);
            return;
        }

        getCommand("smw").setExecutor(new CommandManager());

        try {
            loadWorlds();
        } catch (NullPointerException | IOException ex) {
            Logging.error("Failed to load worlds from config file:");
            ex.printStackTrace();
        }
    }

    private SlimeNMS loadInjector() throws InvalidVersionException {
        String version = Bukkit.getServer().getClass().getPackage().getName();
        String nmsVersion = version.substring(version.lastIndexOf('.') + 1);

        Logging.info("Minecraft version: " + nmsVersion);

        switch (nmsVersion) {
            case "v1_8_R3":
                return new v1_8_R3SlimeNMS();
            default:
                throw new InvalidVersionException(nmsVersion);
        }
    }

    private void loadWorlds() throws IOException {
        ConfigurationSection config = ConfigManager.getFile("worlds").getConfigurationSection("worlds");

        if (config != null) {
            long start = System.currentTimeMillis();
            int loadedWorlds = 0;

            for (String world : config.getKeys(false)) {
                ConfigurationSection worldConfig = config.getConfigurationSection(world);

                if (worldConfig.getBoolean("loadOnStartup", true)) {
                    try {
                        loadWorldFromConfig(worldConfig);
                        loadedWorlds++;
                    } catch (IllegalArgumentException ex) {
                        Logging.error("Couldn't load world " + world + ": " + ex.getMessage() + ".");
                    } catch (UnknownWorldException ex) {
                        Logging.error("Couldn't load world " + world + ": world does not exist, are you sure you've set the correct loader?");
                    } catch (NewerFormatException ex) {
                        Logging.error("Couldn't load world " + world + ": world is serialized in a newer Slime Format version (" + ex.getMessage() + ") that SMW does not understand.");
                    } catch (CorruptedWorldException ex) {
                        Logging.error("Couldn't load world " + world + ": world seems to be corrupted.");

                        ex.printStackTrace();
                    }
                }
            }

            if (loadedWorlds > 0) {
                Logging.info(loadedWorlds + " world" + (loadedWorlds == 1 ? "" : "s") + " loaded in " + (System.currentTimeMillis() - start) + "ms.");
            }
        } else {
            Logging.warning("No worlds found to load!");
        }
    }

    public void loadWorldFromConfig(ConfigurationSection worldConfig) throws UnknownWorldException, IOException, CorruptedWorldException, NewerFormatException {
        if (Bukkit.getWorld(worldConfig.getName()) != null) {
            throw new IllegalArgumentException("world '" + worldConfig.getName() + "' already exists");
        }

        // Config data retrieval
        String loaderString = worldConfig.getString("loader", "");
        SlimeLoader loader = SlimeLoaders.get(loaderString);

        if (loader == null) {
            throw new IllegalArgumentException("unknown loader '" + loaderString + "'");
        }

        String difficultyString = worldConfig.getString("difficulty", "peaceful");
        Difficulty difficulty;

        try {
            difficulty = Enum.valueOf(Difficulty.class, difficultyString.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unknown difficulty '" + difficultyString + "'");
        }

        String spawnLocation = worldConfig.getString("spawn", "0, 255, 0");
        String[] spawnLocationSplit = spawnLocation.split(", ");

        double spawnX, spawnY, spawnZ;

        try {
            spawnX = Double.parseDouble(spawnLocationSplit[0]);
            spawnY = Double.parseDouble(spawnLocationSplit[1]);
            spawnZ = Double.parseDouble(spawnLocationSplit[2]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("invalid spawn location '" + spawnLocation + "'");
        }

        boolean allowMonsters = worldConfig.getBoolean("allowMonsters", true);
        boolean allowAnimals = worldConfig.getBoolean("allowAnimals", true);

        SlimeWorld.SlimeProperties properties = SlimeWorld.SlimeProperties.builder().spawnX(spawnX).spawnY(spawnY).spawnZ(spawnZ)
                .difficulty(difficulty.getValue()).allowMonsters(allowMonsters).allowAnimals(allowAnimals).build();

        // Actual world load
        loadWorld(loader, worldConfig.getName(), properties);
    }

    @Override
    public SlimeWorld loadWorld(SlimeLoader loader, String worldName, SlimeWorld.SlimeProperties properties) throws UnknownWorldException, IOException, CorruptedWorldException, NewerFormatException {
        long start = System.currentTimeMillis();

        Logging.info("Loading world " + worldName + ".");
        byte[] serializedWorld = loader.loadWorld(worldName);
        SlimeWorld world = LoaderUtils.deserializeWorld(loader, worldName, serializedWorld, properties);
        nms.generateWorld(world);

        Logging.info("World " + worldName + " loaded in " + (System.currentTimeMillis() - start) + "ms.");

        return world;
    }
}
