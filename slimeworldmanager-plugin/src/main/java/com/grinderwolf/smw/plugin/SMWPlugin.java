package com.grinderwolf.smw.plugin;

import com.grinderwolf.smw.api.SlimePlugin;
import com.grinderwolf.smw.api.exceptions.CorruptedWorldException;
import com.grinderwolf.smw.api.exceptions.InvalidVersionException;
import com.grinderwolf.smw.api.exceptions.NewerFormatException;
import com.grinderwolf.smw.api.exceptions.UnknownWorldException;
import com.grinderwolf.smw.api.exceptions.UnsupportedWorldException;
import com.grinderwolf.smw.api.exceptions.WorldInUseException;
import com.grinderwolf.smw.api.loaders.SlimeLoader;
import com.grinderwolf.smw.api.world.SlimeWorld;
import com.grinderwolf.smw.nms.CraftSlimeWorld;
import com.grinderwolf.smw.nms.SlimeNMS;
import com.grinderwolf.smw.nms.v1_10_R1.v1_10_R1SlimeNMS;
import com.grinderwolf.smw.nms.v1_11_R1.v1_11_R1SlimeNMS;
import com.grinderwolf.smw.nms.v1_12_R1.v1_12_R1SlimeNMS;
import com.grinderwolf.smw.nms.v1_13_R1.v1_13_R1SlimeNMS;
import com.grinderwolf.smw.nms.v1_13_R2.v1_13_R2SlimeNMS;
import com.grinderwolf.smw.nms.v1_14_R1.v1_14_R1SlimeNMS;
import com.grinderwolf.smw.nms.v1_8_R3.v1_8_R3SlimeNMS;
import com.grinderwolf.smw.nms.v1_9_R1.v1_9_R1SlimeNMS;
import com.grinderwolf.smw.nms.v1_9_R2.v1_9_R2SlimeNMS;
import com.grinderwolf.smw.plugin.commands.CommandManager;
import com.grinderwolf.smw.plugin.config.ConfigManager;
import com.grinderwolf.smw.plugin.loaders.LoaderUtils;
import com.grinderwolf.smw.plugin.log.Logging;
import com.grinderwolf.smw.plugin.world.WorldUnlocker;
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

        try {
            LoaderUtils.registerLoaders();
        } catch (IOException ex) {
            Logging.error("Failed to register data sources:");
            ex.printStackTrace();
        }

        try {
            nms = getNMSBridge();
        } catch (InvalidVersionException ex) {
            Logging.error("Couldn't get nms bridge:");
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
        getServer().getPluginManager().registerEvents(new WorldUnlocker(), this);

        try {
            loadWorlds();
        } catch (NullPointerException | IOException ex) {
            Logging.error("Failed to load worlds from config file:");
            ex.printStackTrace();
        }
    }

    private SlimeNMS getNMSBridge() throws InvalidVersionException {
        String version = Bukkit.getServer().getClass().getPackage().getName();
        String nmsVersion = version.substring(version.lastIndexOf('.') + 1);

        Logging.info("Minecraft version: " + nmsVersion);

        switch (nmsVersion) {
            case "v1_8_R3":
                return new v1_8_R3SlimeNMS();
            case "v1_9_R1":
                return new v1_9_R1SlimeNMS();
            case "v1_9_R2":
                return new v1_9_R2SlimeNMS();
            case "v1_10_R1":
                return new v1_10_R1SlimeNMS();
            case "v1_11_R1":
                return new v1_11_R1SlimeNMS();
            case "v1_12_R1":
                return new v1_12_R1SlimeNMS();
            case "v1_13_R1":
                return new v1_13_R1SlimeNMS();
            case "v1_13_R2":
                return new v1_13_R2SlimeNMS();
            case "v1_14_R1":
                return new v1_14_R1SlimeNMS();
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
                        generateWorld(loadWorldFromConfig(worldConfig));
                        loadedWorlds++;
                    } catch (IllegalArgumentException ex) {
                        Logging.error("Couldn't load world " + world + ": " + ex.getMessage() + ".");
                    } catch (UnknownWorldException ex) {
                        Logging.error("Couldn't load world " + world + ": world does not exist, are you sure you've set the correct data source?");
                    } catch (NewerFormatException ex) {
                        Logging.error("Couldn't load world " + world + ": world is serialized in a newer Slime Format version ("
                                + ex.getMessage() + ") that SMW does not understand.");
                    } catch (WorldInUseException e) {
                        Logging.error("Couldn't load world " + world + ": world is in use! If you are sure this is a mistake, run " +
                                "the command /smw unlock " + world + " " + worldConfig.get("source"));
                    } catch (UnsupportedWorldException e) {
                        Logging.error("Couldn't load world " + world + ": world is meant to be used on a " + (e.isV1_13() ? "1.13 or newer" : "1.12.2 or older") + " server.");
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

    public SlimeWorld loadWorldFromConfig(ConfigurationSection worldConfig) throws UnknownWorldException, IOException,
            CorruptedWorldException, NewerFormatException, WorldInUseException, UnsupportedWorldException {
        if (Bukkit.getWorld(worldConfig.getName()) != null) {
            throw new IllegalArgumentException("world '" + worldConfig.getName() + "' already exists");
        }

        // Config data retrieval
        String loaderString = worldConfig.getString("source", "");
        SlimeLoader loader = getLoader(loaderString);

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

        boolean readOnly = worldConfig.getBoolean("readOnly", false);

        SlimeWorld.SlimeProperties properties = SlimeWorld.SlimeProperties.builder().spawnX(spawnX).spawnY(spawnY).spawnZ(spawnZ)
                .difficulty(difficulty.getValue()).allowMonsters(allowMonsters).allowAnimals(allowAnimals).readOnly(readOnly).build();

        // Actual world load
        return loadWorld(loader, worldConfig.getName(), properties);
    }

    @Override
    public SlimeWorld loadWorld(SlimeLoader loader, String worldName, SlimeWorld.SlimeProperties properties) throws UnknownWorldException,
            IOException, CorruptedWorldException, NewerFormatException, WorldInUseException, UnsupportedWorldException {
        long start = System.currentTimeMillis();

        Logging.info("Loading world " + worldName + ".");
        byte[] serializedWorld = loader.loadWorld(worldName, properties.isReadOnly());
        CraftSlimeWorld world = LoaderUtils.deserializeWorld(loader, worldName, serializedWorld, properties);

        if ((world.isV1_13() && !nms.isV1_13WorldFormat()) || (!world.isV1_13() && nms.isV1_13WorldFormat())) {
            throw new UnsupportedWorldException(worldName, world.isV1_13());
        }

        Logging.info("World " + worldName + " loaded in " + (System.currentTimeMillis() - start) + "ms.");

        return world;
    }

    @Override
    public void generateWorld(SlimeWorld world) {
        nms.generateWorld(world);
    }

    @Override
    public SlimeLoader getLoader(String dataSource) {
        return LoaderUtils.getLoader(dataSource);
    }

    @Override
    public void registerLoader(String dataSource, SlimeLoader loader) {
        LoaderUtils.registerLoader(dataSource, loader);
    }
}
