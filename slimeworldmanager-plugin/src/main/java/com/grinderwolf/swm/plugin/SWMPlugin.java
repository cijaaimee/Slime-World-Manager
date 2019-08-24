package com.grinderwolf.swm.plugin;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntArrayTag;
import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.exceptions.CorruptedWorldException;
import com.grinderwolf.swm.api.exceptions.InvalidVersionException;
import com.grinderwolf.swm.api.exceptions.InvalidWorldException;
import com.grinderwolf.swm.api.exceptions.NewerFormatException;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.UnsupportedWorldException;
import com.grinderwolf.swm.api.exceptions.WorldAlreadyExistsException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;
import com.grinderwolf.swm.api.exceptions.WorldLoadedException;
import com.grinderwolf.swm.api.exceptions.WorldTooBigException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.utils.NibbleArray;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.nms.CraftSlimeChunk;
import com.grinderwolf.swm.nms.CraftSlimeChunkSection;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.grinderwolf.swm.nms.SlimeNMS;
import com.grinderwolf.swm.nms.v1_10_R1.v1_10_R1SlimeNMS;
import com.grinderwolf.swm.nms.v1_11_R1.v1_11_R1SlimeNMS;
import com.grinderwolf.swm.nms.v1_12_R1.v1_12_R1SlimeNMS;
import com.grinderwolf.swm.nms.v1_13_R1.v1_13_R1SlimeNMS;
import com.grinderwolf.swm.nms.v1_13_R2.v1_13_R2SlimeNMS;
import com.grinderwolf.swm.nms.v1_14_R1.v1_14_R1SlimeNMS;
import com.grinderwolf.swm.nms.v1_8_R3.v1_8_R3SlimeNMS;
import com.grinderwolf.swm.nms.v1_9_R1.v1_9_R1SlimeNMS;
import com.grinderwolf.swm.nms.v1_9_R2.v1_9_R2SlimeNMS;
import com.grinderwolf.swm.plugin.commands.CommandManager;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.config.WorldData;
import com.grinderwolf.swm.plugin.config.WorldsConfig;
import com.grinderwolf.swm.plugin.loaders.LoaderUtils;
import com.grinderwolf.swm.plugin.log.Logging;
import com.grinderwolf.swm.plugin.update.Updater;
import com.grinderwolf.swm.plugin.world.WorldImporter;
import com.grinderwolf.swm.plugin.world.WorldUnlocker;
import lombok.Getter;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class SWMPlugin extends JavaPlugin implements SlimePlugin {

    @Getter
    private static SWMPlugin instance;
    @Getter
    private SlimeNMS nms;

    private final List<SlimeWorld> worlds = new ArrayList<>();

    @Override
    public void onLoad() {
        instance = this;

        try {
            ConfigManager.initialize();
        } catch (NullPointerException | IOException | ObjectMappingException ex) {
            Logging.error("Failed to load config files:");
            ex.printStackTrace();
            return;
        }

        try {
            LoaderUtils.registerLoaders();
        } catch (IOException ex) {
            Logging.error("Failed to register data sources:");
            ex.printStackTrace();
            return;
        }

        try {
            nms = getNMSBridge();
        } catch (InvalidVersionException ex) {
            Logging.error("Couldn't get nms bridge:");
            ex.printStackTrace();
            return;
        }

        loadWorlds();

        // Default world override
        try {
            Properties props = new Properties();

            props.load(new FileInputStream("server.properties"));
            String defaultWorldName = props.getProperty("level-name");

            SlimeWorld defaultWorld = worlds.stream().filter(world -> world.getName().equals(defaultWorldName)).findFirst().orElse(null);
            SlimeWorld netherWorld = (getServer().getAllowNether() ? worlds.stream().filter(world -> world.getName().equals(defaultWorldName + "_nether")).findFirst().orElse(null) : null);
            SlimeWorld endWorld = (getServer().getAllowEnd() ? worlds.stream().filter(world -> world.getName().equals(defaultWorldName + "_the_end")).findFirst().orElse(null) : null);

            nms.setDefaultWorlds(defaultWorld, netherWorld, endWorld);
        } catch (IOException ex) {
            Logging.error("Failed to retrieve default world name:");
            ex.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        if (nms == null) {
            this.setEnabled(false);
            return;
        }

        getCommand("swm").setExecutor(new CommandManager());
        getServer().getPluginManager().registerEvents(new WorldUnlocker(), this);
        getServer().getPluginManager().registerEvents(new Updater(), this);

        for (SlimeWorld world : worlds) {
            if (Bukkit.getWorld(world.getName()) == null) {
                generateWorld(world);
            }
        }

        worlds.clear();
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

    private void loadWorlds() {
        WorldsConfig config = ConfigManager.getWorldConfig();

        for (Map.Entry<String, WorldData> entry : config.getWorlds().entrySet()) {
            String worldName = entry.getKey();
            WorldData worldData = entry.getValue();

            if (worldData.isLoadOnStartup()) {
                try {
                    SlimeLoader loader = getLoader(worldData.getDataSource());

                    if (loader == null) {
                        throw new IllegalArgumentException("invalid data source " + worldData.getDataSource() + "");
                    }

                    SlimeWorld.SlimeProperties properties = worldData.toProperties();
                    SlimeWorld world = loadWorld(loader, worldName, properties);

                    worlds.add(world);
                } catch (IllegalArgumentException ex) {
                    Logging.error("Couldn't load world " + worldName + ": " + ex.getMessage());
                } catch (UnknownWorldException ex) {
                    Logging.error("Couldn't load world " + worldName + ": world does not exist, are you sure you've set the correct data source?");
                } catch (NewerFormatException ex) {
                    Logging.error("Couldn't load world " + worldName + ": world is serialized in a newer Slime Format version ("
                            + ex.getMessage() + ") that SWM does not understand.");
                } catch (WorldInUseException e) {
                    Logging.error("Couldn't load world " + worldName + ": world is in use! If you are sure this is a mistake, run the command /swm unlock " + worldName);
                } catch (UnsupportedWorldException e) {
                    Logging.error("Couldn't load world " + worldName + ": world is meant to be used on a " + (e.isV1_13() ? "1.13 or newer" : "1.12.2 or older") + " server.");
                } catch (CorruptedWorldException ex) {
                    Logging.error("Couldn't load world " + worldName + ": world seems to be corrupted.");

                    ex.printStackTrace();
                } catch (IOException ex) {
                    Logging.error("Couldn't load world " + worldName + ":");

                    ex.printStackTrace();
                }
            }
        }

        config.save();
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
    public SlimeWorld createEmptyWorld(SlimeLoader loader, String worldName, SlimeWorld.SlimeProperties properties) throws WorldAlreadyExistsException, IOException {
        if (loader.worldExists(worldName)) {
            throw new WorldAlreadyExistsException(worldName);
        }

        long start = System.currentTimeMillis();

        Logging.info("Creating empty world " + worldName + ".");

        // The world must contain at least one chunk
        SlimeChunkSection[] chunkSections = new SlimeChunkSection[16];
        chunkSections[0] = new CraftSlimeChunkSection(new byte[4096], new NibbleArray(4096), null, null, new NibbleArray(4096), new NibbleArray(4096));
        CompoundTag heightMaps;
        int[] biomes;

        if (nms.isV1_13WorldFormat()) {
            heightMaps = null;
            biomes = new int[256];
        } else {
            heightMaps = new CompoundTag("", new CompoundMap());
            heightMaps.getValue().put("heightMap", new IntArrayTag("heightMap", new int[256]));
            biomes = new int[64];

        }

        SlimeChunk chunk = new CraftSlimeChunk(worldName, 0, 0, chunkSections, heightMaps, biomes, new ArrayList<>(), new ArrayList<>());
        Map<Long, SlimeChunk> chunkMap = new HashMap<>();

        chunkMap.put(0L, chunk);

        CraftSlimeWorld world = new CraftSlimeWorld(loader, worldName, chunkMap, new CompoundTag("", new CompoundMap()), nms.isV1_13WorldFormat(), properties);
        loader.saveWorld(worldName, world.serialize(), !properties.isReadOnly());

        Logging.info("World " + worldName + " created in " + (System.currentTimeMillis() - start) + "ms.");

        return world;
    }

    @Override
    public void generateWorld(SlimeWorld world) {
        nms.generateWorld(world);
    }

    @Override
    public void migrateWorld(String worldName, SlimeLoader currentLoader, SlimeLoader newLoader) throws IOException, WorldInUseException, WorldAlreadyExistsException, UnknownWorldException {
        if (newLoader.worldExists(worldName)) {
            throw new WorldAlreadyExistsException(worldName);
        }

        World bukkitWorld = Bukkit.getWorld(worldName);

        boolean leaveLock = false;

        if (bukkitWorld != null) {
            // Make sure the loaded world really is a SlimeWorld and not a normal Bukkit world
            CraftSlimeWorld slimeWorld = (CraftSlimeWorld) SWMPlugin.getInstance().getNms().getSlimeWorld(bukkitWorld);

            if (slimeWorld != null && currentLoader.equals(slimeWorld.getLoader())) {
                slimeWorld.setLoader(newLoader);

                if (!slimeWorld.getProperties().isReadOnly()) { // We have to manually unlock the world so no WorldInUseException is thrown
                    currentLoader.unlockWorld(worldName);
                    leaveLock = true;
                }
            }
        }

        byte[] serializedWorld = currentLoader.loadWorld(worldName, false);

        newLoader.saveWorld(worldName, serializedWorld, leaveLock);
        currentLoader.deleteWorld(worldName);
    }

    @Override
    public SlimeLoader getLoader(String dataSource) {
        return LoaderUtils.getLoader(dataSource);
    }

    @Override
    public void registerLoader(String dataSource, SlimeLoader loader) {
        LoaderUtils.registerLoader(dataSource, loader);
    }

    @Override
    public void importWorld(File worldDir, String worldName, SlimeLoader loader) throws WorldAlreadyExistsException,
            InvalidWorldException, WorldLoadedException, WorldTooBigException, IOException {
        if (loader.worldExists(worldName)) {
            throw new WorldAlreadyExistsException(worldName);
        }

        World bukkitWorld = Bukkit.getWorld(worldDir.getName());

        if (bukkitWorld != null && nms.getSlimeWorld(bukkitWorld) == null) {
            throw new WorldLoadedException(worldDir.getName());
        }

        CraftSlimeWorld world = WorldImporter.readFromDirectory(worldDir);

        byte[] serializedWorld;

        try {
            serializedWorld = world.serialize();
        } catch (IndexOutOfBoundsException ex) {
            throw new WorldTooBigException(worldDir.getName());
        }

        loader.saveWorld(worldName, serializedWorld, false);
    }
}
