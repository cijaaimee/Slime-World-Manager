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
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R1.CraftWorld;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.io.IOException;
import java.util.Arrays;

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
    public void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld) throws IOException {
        if (normalWorld != null) {
            World.Environment env = World.Environment.valueOf(normalWorld.getPropertyMap().getString(SlimeProperties.ENVIRONMENT).toUpperCase());

            if (env != World.Environment.NORMAL) {
                LOGGER.warn("The environment for the default world must always be 'NORMAL'.");
            }

            MinecraftServer mcServer = MinecraftServer.getServer();
            Convertable.ConversionSession conversionSession = getConversionSession(normalWorld.getName(), mcServer, WorldDimension.OVERWORLD);
            CustomNBTStorage dataManager = new CustomNBTStorage(normalWorld, conversionSession);
            DimensionManager dimensionManager = mcServer.f.a().fromId(env.getId());
            WorldDataServer worldData = (WorldDataServer)dataManager.getWorldData();
            ResourceKey<net.minecraft.server.v1_16_R1.World> worldKey = ResourceKey.a(IRegistry.ae, new MinecraftKey(normalWorld.getName()));
            defaultWorld = new CustomWorldServer((CraftSlimeWorld) normalWorld, dataManager, conversionSession, dimensionManager, env, worldData, worldKey, DimensionManager.OVERWORLD, Arrays.asList(new MobSpawnerCat()), false, false);
        }

        if (netherWorld != null) {
            World.Environment env = World.Environment.valueOf(netherWorld.getPropertyMap().getString(SlimeProperties.ENVIRONMENT).toUpperCase());
            MinecraftServer mcServer = MinecraftServer.getServer();
            Convertable.ConversionSession conversionSession = getConversionSession(netherWorld.getName(), mcServer, WorldDimension.THE_NETHER);
            CustomNBTStorage dataManager = new CustomNBTStorage(netherWorld, conversionSession);
            DimensionManager dimensionManager = mcServer.f.a().fromId(env.getId());
            WorldDataServer worldData = (WorldDataServer)dataManager.getWorldData();
            ResourceKey<net.minecraft.server.v1_16_R1.World> worldKey = ResourceKey.a(IRegistry.ae, new MinecraftKey(netherWorld.getName()));
            defaultNetherWorld = new CustomWorldServer((CraftSlimeWorld) netherWorld, dataManager, conversionSession, dimensionManager, env, worldData, worldKey, DimensionManager.OVERWORLD, Arrays.asList(new MobSpawnerCat()), false, false);
        }

        if (endWorld != null) {
            World.Environment env = World.Environment.valueOf(endWorld.getPropertyMap().getString(SlimeProperties.ENVIRONMENT).toUpperCase());
            MinecraftServer mcServer = MinecraftServer.getServer();
            Convertable.ConversionSession conversionSession = getConversionSession(endWorld.getName(), mcServer, WorldDimension.THE_END);
            CustomNBTStorage dataManager = new CustomNBTStorage(endWorld, conversionSession);
            DimensionManager dimensionManager = mcServer.f.a().fromId(env.getId());
            WorldDataServer worldData = (WorldDataServer)dataManager.getWorldData();
            ResourceKey<net.minecraft.server.v1_16_R1.World> worldKey = ResourceKey.a(IRegistry.ae, new MinecraftKey(endWorld.getName()));
            defaultEndWorld = new CustomWorldServer((CraftSlimeWorld) endWorld, dataManager, conversionSession, dimensionManager, env, worldData, worldKey, DimensionManager.OVERWORLD, Arrays.asList(new MobSpawnerCat()), false, false);

        }

        loadingDefaultWorlds = false;
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
        mcServer.initWorld(server, server.worldDataServer, null, server.worldDataServer.getGeneratorSettings());

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

        ResourceKey<net.minecraft.server.v1_16_R1.World> worldKey = ResourceKey.a(IRegistry.ae, new MinecraftKey(worldName));

        MinecraftServer mcServer = MinecraftServer.getServer();

        Convertable.ConversionSession conversionSession = getConversionSession(worldName, mcServer, WorldDimension.OVERWORLD);

        CustomNBTStorage dataManager = new CustomNBTStorage(world, conversionSession);


        World.Environment env = World.Environment.valueOf(world.getPropertyMap().getString(SlimeProperties.ENVIRONMENT).toUpperCase());

        DimensionManager dimensionManager = mcServer.f.a().fromId(env.getId());
        WorldDataServer worldData = (WorldDataServer)dataManager.getWorldData();

        CustomWorldServer server = null;

        try {
            LOGGER.debug("Server-pre: " + server);
            LOGGER.debug("Server-world: " + world.getName());
            LOGGER.debug("Server-DM: " + dimensionManager);
            LOGGER.debug("Server-env: " + env.getId());
            LOGGER.debug("Server-CG: " + worldData.getGeneratorSettings());
            LOGGER.debug("Server-WS: " + worldData);
            LOGGER.debug("Server-Dir: " + conversionSession.folder.toString());
            server = new CustomWorldServer((CraftSlimeWorld) world, dataManager, conversionSession, dimensionManager, env, worldData, worldKey, DimensionManager.OVERWORLD, Arrays.asList(new MobSpawnerCat()), false, true);
            LOGGER.debug("SLIME-WORLD-NAME: " + server.getSlimeWorld().getName());
            LOGGER.debug("SERVER-WORLD-NAME: " + server.getWorld().getName());
            LOGGER.debug("WORLD-DATA-SERVER: " + worldData);
            LOGGER.debug("SPAWN: " + server.getWorld().getSpawnLocation());
            LOGGER.debug("SPAWN-2: " + server.getSpawn());
            LOGGER.debug("SLIMEWORLD-NAME: " + worldName);
        } catch(IOException e) {
            LOGGER.debug("Server-error: " + server);
            e.printStackTrace();
            return null;
        }
        LOGGER.debug("Server-post: " + server);
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
        MinecraftServer.getServer().worldServer.put(server.getDimensionKey(), server);

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