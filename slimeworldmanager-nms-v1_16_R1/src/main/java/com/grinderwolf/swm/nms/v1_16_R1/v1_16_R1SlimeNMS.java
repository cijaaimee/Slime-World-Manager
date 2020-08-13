package com.grinderwolf.swm.nms.v1_16_R1;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.grinderwolf.swm.nms.SlimeNMS;
import net.minecraft.server.v1_16_R1.BlockPosition;
import net.minecraft.server.v1_16_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R1.ChunkProviderServer;
import net.minecraft.server.v1_16_R1.Convertable;
import net.minecraft.server.v1_16_R1.DataConverterRegistry;
import net.minecraft.server.v1_16_R1.DataFixTypes;
import net.minecraft.server.v1_16_R1.DimensionManager;
import net.minecraft.server.v1_16_R1.GameProfileSerializer;
import net.minecraft.server.v1_16_R1.IRegistry;
import net.minecraft.server.v1_16_R1.MinecraftKey;
import net.minecraft.server.v1_16_R1.MinecraftServer;
import net.minecraft.server.v1_16_R1.MobSpawnerCat;
import net.minecraft.server.v1_16_R1.NBTTagCompound;
import net.minecraft.server.v1_16_R1.ResourceKey;
import net.minecraft.server.v1_16_R1.TicketType;
import net.minecraft.server.v1_16_R1.Unit;
import net.minecraft.server.v1_16_R1.WorldDataServer;
import net.minecraft.server.v1_16_R1.WorldDimension;
import net.minecraft.server.v1_16_R1.WorldServer;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_16_R1.CraftServer;
import org.bukkit.craftbukkit.v1_16_R1.CraftWorld;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

@Getter
public class v1_16_R1SlimeNMS implements SlimeNMS {

    private static final Logger LOGGER = LogManager.getLogger("SWM");

    private final byte worldVersion = 0x05;

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
            defaultWorld = initDefaultWorld(normalWorld, WorldDimension.OVERWORLD, DimensionManager.OVERWORLD, true);
        }

        if (netherWorld != null) {
            defaultNetherWorld = initDefaultWorld(netherWorld, WorldDimension.THE_NETHER, DimensionManager.THE_NETHER, false);
        }

        if (endWorld != null) {
            defaultEndWorld = initDefaultWorld(endWorld, WorldDimension.THE_END, DimensionManager.THE_END, false);
        }

        loadingDefaultWorlds = false;
    }

    private CustomWorldServer initDefaultWorld(
        SlimeWorld world,
        ResourceKey<WorldDimension> worldDimensionKey,
        ResourceKey<DimensionManager> dimensionManagerKey,
        boolean normalOnly
    ) {
        World.Environment env = World.Environment.valueOf(world.getPropertyMap().getString(SlimeProperties.ENVIRONMENT).toUpperCase());

        if(normalOnly && env != World.Environment.NORMAL) {
            LOGGER.warn("The environment for the default world must always be 'NORMAL'.");
        }

        MinecraftServer mcServer = MinecraftServer.getServer();
        Convertable.ConversionSession conversionSession = getConversionSession(world.getName(), mcServer, worldDimensionKey);
        CustomNBTStorage dataManager = new CustomNBTStorage(world, conversionSession);
        DimensionManager dimensionManager = mcServer.f.a().fromId(env.getId());
        WorldDataServer worldData = (WorldDataServer)dataManager.getWorldData();
        ResourceKey<net.minecraft.server.v1_16_R1.World> worldKey = ResourceKey.a(IRegistry.ae, new MinecraftKey(world.getName()));

        return new CustomWorldServer((CraftSlimeWorld) world, dataManager, conversionSession, dimensionManager, env, worldData, worldKey, dimensionManagerKey, Collections.emptyList());
    }

    @SneakyThrows
    public static Convertable.ConversionSession getConversionSession(String worldName, MinecraftServer mcServer, ResourceKey<WorldDimension> dimensionKey) {
        return Convertable.a(mcServer.server.getWorldContainer().toPath()).c(worldName, dimensionKey);
    }

    @SneakyThrows
    @Override
    public void generateWorld(SlimeWorld world) {
        CustomWorldServer server = createNMSWorld(world);
        MinecraftServer mcServer = MinecraftServer.getServer();

        LOGGER.info("Loading world " + server.getWorld().getName());
        long startTime = System.currentTimeMillis();

        server.setReady(true);
        server.worldDataServer.c(true);

        LOGGER.debug("Start initWorld");
        long now = System.currentTimeMillis();
        mcServer.initWorld(server, server.worldDataServer, null, server.worldDataServer.getGeneratorSettings());
        LOGGER.debug("End initWorld (" + (System.currentTimeMillis() - now) + "ms)");

        mcServer.server.addWorld(server.getWorld());
        mcServer.worldServer.put(server.getDimensionKey(), server);

        Bukkit.getPluginManager().callEvent(new WorldInitEvent(server.getWorld()));

        if (server.getWorld().getKeepSpawnInMemory()) {
            LOGGER.debug("Preparing start region for dimension '{}'", server.getWorld().getName());
            BlockPosition spawn = server.getSpawn();
            ChunkProviderServer provider = server.getChunkProvider();
            provider.addTicket(TicketType.START, new ChunkCoordIntPair(spawn), 11, Unit.INSTANCE);
        }

        Bukkit.getPluginManager().callEvent(new WorldLoadEvent(server.getWorld()));

        LOGGER.info("World " + server.getWorld().getName() + " loaded in " + (System.currentTimeMillis() - startTime) + "ms.");
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

    @Override
    public CustomWorldServer createNMSWorld(SlimeWorld world) {
        String worldName = world.getName();

        if (Bukkit.getWorld(worldName) != null) {
            throw new IllegalArgumentException("World " + worldName + " already exists! Maybe it's an outdated SlimeWorld object?");
        }

        World.Environment env = World.Environment.valueOf(world.getPropertyMap().getString(SlimeProperties.ENVIRONMENT).toUpperCase());
        String minecraftEnvString;

        switch(env) {
            case NORMAL:
                minecraftEnvString = "overworld";
                break;
            case NETHER:
                minecraftEnvString = "the_nether";
                break;
            case THE_END:
                minecraftEnvString = "the_end";
                break;
            default:
                throw new IllegalArgumentException("Unknown dimension supplied");
        }

        ResourceKey<DimensionManager> dimensionManagerKey = ResourceKey.a(IRegistry.ad, new MinecraftKey(minecraftEnvString));
        ResourceKey<WorldDimension> worldDimensionKey = ResourceKey.a(IRegistry.af, new MinecraftKey(minecraftEnvString));
        ResourceKey<net.minecraft.server.v1_16_R1.World> worldKey = ResourceKey.a(IRegistry.ae, new MinecraftKey(worldName));

        MinecraftServer mcServer = MinecraftServer.getServer();
        Convertable.ConversionSession conversionSession = getConversionSession(worldName, mcServer, worldDimensionKey);
        CustomNBTStorage dataManager = new CustomNBTStorage(world, conversionSession);

        DimensionManager dimensionManager = mcServer.f.a().a(dimensionManagerKey);
        WorldDataServer worldData = (WorldDataServer)dataManager.getWorldData();

        CustomWorldServer server = null;

        LOGGER.debug("Server-pre: " + server);
        LOGGER.debug("Server-world: " + world.getName());
        LOGGER.debug("Server-DM: " + dimensionManager);
        LOGGER.debug("Server-env: " + env.getId());
        LOGGER.debug("Server-CG: " + worldData.getGeneratorSettings());
        LOGGER.debug("Server-WS: " + worldData);
        LOGGER.debug("Server-Dir: " + conversionSession.folder.toString());
        LOGGER.debug("Server-DM: " + dimensionManager);
        LOGGER.debug("Server-DM-Key: " + dimensionManagerKey);
        LOGGER.debug("Server-WD-Key: " + worldDimensionKey);

        server = new CustomWorldServer((CraftSlimeWorld) world, dataManager, conversionSession, dimensionManager, env, worldData, worldKey, dimensionManagerKey, Collections.emptyList());

        LOGGER.debug("SLIME-WORLD-NAME: " + server.getSlimeWorld().getName());
        LOGGER.debug("SERVER-WORLD-NAME: " + server.getWorld().getName());
        LOGGER.debug("WORLD-DATA-SERVER: " + worldData);
        LOGGER.debug("SPAWN: " + server.getWorld().getSpawnLocation());
        LOGGER.debug("SPAWN-2: " + server.getSpawn());
        LOGGER.debug("SLIMEWORLD-NAME: " + worldName);

        return server;
    }

    @Override
    public void addWorldToServerList(Object worldObject) {
        if (!(worldObject instanceof WorldServer)) {
            throw new IllegalArgumentException("World object must be an instance of WorldServer!");
        }

        CustomWorldServer server = (CustomWorldServer) worldObject;
        String worldName = server.getWorld().getName();

        if (Bukkit.getWorld(worldName) != null) {
            throw new IllegalArgumentException("World " + worldName + " already exists! Maybe it's an outdated SlimeWorld object?");
        }

        LOGGER.info("Async Loading world " + worldName);
        long startTime = System.currentTimeMillis();

        server.setReady(true);
        MinecraftServer mcServer = MinecraftServer.getServer();

        mcServer.server.addWorld(server.getWorld());
        mcServer.worldServer.put(server.getDimensionKey(), server);

        Bukkit.getPluginManager().callEvent(new WorldInitEvent(server.getWorld()));
        Bukkit.getPluginManager().callEvent(new WorldLoadEvent(server.getWorld()));

        SlimePropertyMap worldProperties = server.getSlimeWorld().getPropertyMap();

        server.getWorld().setSpawnLocation(
            worldProperties.getInt(SlimeProperties.SPAWN_X),
            worldProperties.getInt(SlimeProperties.SPAWN_Y),
            worldProperties.getInt(SlimeProperties.SPAWN_Z)
        );

        LOGGER.info("Async World " + worldName + " loaded in " + (System.currentTimeMillis() - startTime) + "ms.");
    }
}