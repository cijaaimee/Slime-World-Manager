package com.grinderwolf.swm.nms.v1_16_R1;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.grinderwolf.swm.nms.SlimeNMS;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import net.minecraft.server.v1_16_R1.BlockPosition;
import net.minecraft.server.v1_16_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R1.ChunkGenerator;
import net.minecraft.server.v1_16_R1.ChunkProviderServer;
import net.minecraft.server.v1_16_R1.Convertable;
import net.minecraft.server.v1_16_R1.DataConverterRegistry;
import net.minecraft.server.v1_16_R1.DataFixTypes;
import net.minecraft.server.v1_16_R1.DedicatedServer;
import net.minecraft.server.v1_16_R1.DimensionManager;
import net.minecraft.server.v1_16_R1.DynamicOpsNBT;
import net.minecraft.server.v1_16_R1.EnumDifficulty;
import net.minecraft.server.v1_16_R1.EnumGamemode;
import net.minecraft.server.v1_16_R1.GameProfileSerializer;
import net.minecraft.server.v1_16_R1.GameRules;
import net.minecraft.server.v1_16_R1.GeneratorSettings;
import net.minecraft.server.v1_16_R1.IRegistry;
import net.minecraft.server.v1_16_R1.IRegistryCustom;
import net.minecraft.server.v1_16_R1.LevelVersion;
import net.minecraft.server.v1_16_R1.MinecraftKey;
import net.minecraft.server.v1_16_R1.MinecraftServer;
import net.minecraft.server.v1_16_R1.MobSpawnerCat;
import net.minecraft.server.v1_16_R1.NBTBase;
import net.minecraft.server.v1_16_R1.NBTTagCompound;
import net.minecraft.server.v1_16_R1.RegistryMaterials;
import net.minecraft.server.v1_16_R1.ResourceKey;
import net.minecraft.server.v1_16_R1.SharedConstants;
import net.minecraft.server.v1_16_R1.TicketType;
import net.minecraft.server.v1_16_R1.Unit;
import net.minecraft.server.v1_16_R1.WorldDataServer;
import net.minecraft.server.v1_16_R1.WorldDimension;
import net.minecraft.server.v1_16_R1.WorldServer;
import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraft.server.v1_16_R1.WorldSettings;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.craftbukkit.libs.org.apache.commons.io.FileUtils;
import org.bukkit.craftbukkit.v1_16_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_16_R1.CraftServer;
import org.bukkit.craftbukkit.v1_16_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R1.util.CraftMagicNumbers;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

@Getter
public class v1_16_R1SlimeNMS implements SlimeNMS {

    private static final Logger LOGGER = LogManager.getLogger("SWM");
    private static final File UNIVERSE_DIR;
    public static Convertable CONVERTABLE;

    static {
        Path path;

        try{
            path = Files.createTempDirectory("swm-" + UUID.randomUUID().toString().substring(0, 5) + "-");
        }catch(IOException ex) {
            LOGGER.log(Level.FATAL, "Failed to create temp directory", ex);
            path = null;
            System.exit(1);
        }

        UNIVERSE_DIR = path.toFile();
        CONVERTABLE = Convertable.a(path);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            try {
                FileUtils.deleteDirectory(UNIVERSE_DIR);
            } catch (IOException ex) {
                LOGGER.log(Level.FATAL, "Failed to delete temp directory", ex);
            }

        }));
    }

    private final byte worldVersion = 0x06;

    private boolean loadingDefaultWorlds = true; // If true, the addWorld method will not be skipped

    private CustomWorldServer defaultWorld;
    private CustomWorldServer defaultNetherWorld;
    private CustomWorldServer defaultEndWorld;

    public v1_16_R1SlimeNMS() {
        try {
            CraftCLSMBridge.initialize(this);
        } catch (NoClassDefFoundError ex) {
            LOGGER.error("Failed to find ClassModifier classes. Are you sure you installed it correctly?");
            System.exit(1); // No ClassModifier, no party
        }
    }

    @Override
    public void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld) {
        if (normalWorld != null) {
            defaultWorld = createDefaultWorld(normalWorld, WorldDimension.OVERWORLD, net.minecraft.server.v1_16_R1.World.OVERWORLD, ResourceKey.a(IRegistry.ad, new MinecraftKey(normalWorld.getName().toLowerCase(Locale.ENGLISH))));
        }

        if (netherWorld != null) {
            defaultNetherWorld = createDefaultWorld(netherWorld, WorldDimension.THE_NETHER, net.minecraft.server.v1_16_R1.World.THE_NETHER, ResourceKey.a(IRegistry.ad, new MinecraftKey(netherWorld.getName().toLowerCase(Locale.ENGLISH))));
        }

        if (endWorld != null) {
            defaultEndWorld = createDefaultWorld(endWorld, WorldDimension.THE_END, net.minecraft.server.v1_16_R1.World.THE_END, ResourceKey.a(IRegistry.ad, new MinecraftKey(endWorld.getName().toLowerCase(Locale.ENGLISH))));
        }

        loadingDefaultWorlds = false;
    }

    private CustomWorldServer createDefaultWorld(SlimeWorld world, ResourceKey<WorldDimension> dimensionKey, ResourceKey<net.minecraft.server.v1_16_R1.World> worldKey, ResourceKey<net.minecraft.server.v1_16_R1.DimensionManager> dmKey) {
        WorldDataServer worldDataServer = createWorldData(world.getName(), world.getExtraData());

        RegistryMaterials<WorldDimension> registryMaterials = worldDataServer.getGeneratorSettings().e();
        WorldDimension worldDimension = registryMaterials.a(dimensionKey);
        DimensionManager dimensionManager = worldDimension.b();
        ChunkGenerator chunkGenerator = worldDimension.c();

        World.Environment environment = getEnvironment(world);

        if (dimensionKey == WorldDimension.OVERWORLD && environment != World.Environment.NORMAL) {
            LOGGER.warn("The environment for the default world should always be 'NORMAL'.");
        }

        try {
            return new CustomWorldServer((CraftSlimeWorld) world, worldDataServer, worldKey, dimensionKey, dmKey, dimensionManager, chunkGenerator, environment);
        } catch (IOException ex) {
            throw new RuntimeException(ex); // TODO do something better with this?
        }
    }

    @Override
    public void generateWorld(SlimeWorld world) {
        String worldName = world.getName();

        if (Bukkit.getWorld(worldName) != null) {
            throw new IllegalArgumentException("World " + worldName + " already exists! Maybe it's an outdated SlimeWorld object?");
        }

        WorldDataServer worldDataServer = createWorldData(world.getName(), world.getExtraData());
        RegistryMaterials<WorldDimension> materials = worldDataServer.getGeneratorSettings().e();
        WorldDimension worldDimension = materials.a(WorldDimension.OVERWORLD);
        DimensionManager dimensionManager = worldDimension.b();
        ChunkGenerator chunkGenerator = worldDimension.c();

        ResourceKey<net.minecraft.server.v1_16_R1.World> worldKey = ResourceKey.a(IRegistry.ae, new MinecraftKey(worldName.toLowerCase(Locale.ENGLISH)));
        ResourceKey<net.minecraft.server.v1_16_R1.DimensionManager> dmKey = ResourceKey.a(IRegistry.ad, new MinecraftKey(worldName.toLowerCase(Locale.ENGLISH)));

        World.Environment environment = getEnvironment(world);

        CustomWorldServer server;

        try {
            server = new CustomWorldServer((CraftSlimeWorld) world, worldDataServer, worldKey, WorldDimension.OVERWORLD, dmKey, dimensionManager, chunkGenerator, environment);
        } catch (IOException ex) {
            throw new RuntimeException(ex); // TODO do something better with this?
        }

        LOGGER.info("Loading world " + worldName);
        long startTime = System.currentTimeMillis();

        server.setReady(true);

        MinecraftServer mcServer = MinecraftServer.getServer();
        mcServer.initWorld(server, worldDataServer, mcServer.getSaveData(), worldDataServer.getGeneratorSettings());

        mcServer.server.addWorld(server.getWorld());
        mcServer.worldServer.put(worldKey, server);

        Bukkit.getPluginManager().callEvent(new WorldInitEvent(server.getWorld()));
        mcServer.loadSpawn(server.getChunkProvider().playerChunkMap.worldLoadListener, server);
        Bukkit.getPluginManager().callEvent(new WorldLoadEvent(server.getWorld()));

        LOGGER.info("World " + worldName + " loaded in " + (System.currentTimeMillis() - startTime) + "ms.");
    }

    private World.Environment getEnvironment(SlimeWorld world) {
        return World.Environment.valueOf(world.getPropertyMap().getString(SlimeProperties.ENVIRONMENT).toUpperCase());
    }

    private WorldDataServer createWorldData(String worldName, CompoundTag extraData) {
        WorldDataServer worldDataServer;
        NBTTagCompound extraTag = (NBTTagCompound) Converter.convertTag(extraData);
        MinecraftServer mcServer = MinecraftServer.getServer();

        if (extraTag.hasKeyOfType("LevelData", CraftMagicNumbers.NBT.TAG_COMPOUND)) {
            NBTTagCompound levelData = extraTag.getCompound("LevelData");
            int dataVersion = levelData.hasKeyOfType("DataVersion", 99) ? levelData.getInt("DataVersion") : -1;
            Dynamic<NBTBase> dynamic = mcServer.getDataFixer().update(DataFixTypes.LEVEL.a(),
                    new Dynamic<>(DynamicOpsNBT.a, levelData), dataVersion, SharedConstants.getGameVersion()
                            .getWorldVersion());
            GeneratorSettings generatorSettings = GeneratorSettings.a();
            Lifecycle lifecycle = Lifecycle.stable();
            LevelVersion levelVersion = LevelVersion.a(dynamic);
            WorldSettings worldSettings = WorldSettings.a(dynamic, mcServer.datapackconfiguration);

            worldDataServer = WorldDataServer.a(dynamic, mcServer.getDataFixer(), dataVersion, null,
                    worldSettings, levelVersion, generatorSettings, lifecycle);
        } else {
            EnumDifficulty difficulty = ((DedicatedServer) mcServer).getDedicatedServerProperties().difficulty;
            EnumGamemode defaultGamemode = ((DedicatedServer) mcServer).getDedicatedServerProperties().gamemode;
            WorldSettings worldSettings = new WorldSettings(worldName, defaultGamemode, false,
                    difficulty, false, new GameRules(), mcServer.datapackconfiguration);
            worldDataServer = new WorldDataServer(worldSettings, ((DedicatedServer) mcServer)
                    .getDedicatedServerProperties().generatorSettings, Lifecycle.stable());
        }

        worldDataServer.checkName(worldName);
        worldDataServer.a(mcServer.getServerModName(), mcServer.getModded().isPresent());
        worldDataServer.c(true);

        return worldDataServer;
    }

    @Override
    public SlimeWorld getSlimeWorld(World world) {
        CraftWorld craftWorld = (CraftWorld) world;

        if (!(craftWorld.getHandle() instanceof CustomWorldServer)) {
            return null;
        }

        CustomWorldServer worldServer = (CustomWorldServer) craftWorld.getHandle();
        return worldServer.getSlimeWorld();
    }

    @Override
    public CompoundTag convertChunk(CompoundTag tag) {
        NBTTagCompound nmsTag = (NBTTagCompound) Converter.convertTag(tag);
        int version = nmsTag.getInt("DataVersion");

        NBTTagCompound newNmsTag = GameProfileSerializer.a(DataConverterRegistry.a(), DataFixTypes.CHUNK, nmsTag, version);

        return (CompoundTag) Converter.convertTag("", newNmsTag);
    }
}