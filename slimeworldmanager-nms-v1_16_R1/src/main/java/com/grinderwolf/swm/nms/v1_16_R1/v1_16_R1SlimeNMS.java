package com.grinderwolf.swm.nms.v1_16_R1;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.grinderwolf.swm.nms.SlimeNMS;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_16_R1.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R1.CraftWorld;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.io.File;
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
    public void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld) {
        if (normalWorld != null) {
            World.Environment env = World.Environment.valueOf(normalWorld.getPropertyMap().getString(SlimeProperties.ENVIRONMENT).toUpperCase());

            if (env != World.Environment.NORMAL) {
                LOGGER.warn("The environment for the default world must always be 'NORMAL'.");
            }
            
//            defaultWorld = new CustomWorldServer((CraftSlimeWorld) normalWorld, new CustomNBTStorage(normalWorld), DimensionManager.OVERWORLD, World.Environment.NORMAL);
        }

        if (netherWorld != null) {
            World.Environment env = World.Environment.valueOf(netherWorld.getPropertyMap().getString(SlimeProperties.ENVIRONMENT).toUpperCase());
//            defaultNetherWorld = new CustomWorldServer((CraftSlimeWorld) netherWorld, new CustomNBTStorage(netherWorld), DimensionManager.a(env.getId()), env);
        }

        if (endWorld != null) {
            World.Environment env = World.Environment.valueOf(endWorld.getPropertyMap().getString(SlimeProperties.ENVIRONMENT).toUpperCase());
//            defaultEndWorld = new CustomWorldServer((CraftSlimeWorld) endWorld, new CustomNBTStorage(endWorld), DimensionManager.a(env.getId()), env);
        }

        loadingDefaultWorlds = false;
    }

    @Override
    public void generateWorld(SlimeWorld world) {
        String worldName = world.getName();

        if (Bukkit.getWorld(worldName) != null) {
            throw new IllegalArgumentException("World " + worldName + " already exists! Maybe it's an outdated SlimeWorld object?");
        }

        MinecraftServer mcServer = MinecraftServer.getServer();

        CustomNBTStorage dataManager = null;
        try {
            dataManager = new CustomNBTStorage(world, mcServer.D().convertable, mcServer.dataConverterManager);
        } catch(IOException e) {
            e.printStackTrace();
        }

        int dimension = CraftWorld.CUSTOM_DIMENSION_OFFSET + mcServer.worldServer.size();
        boolean used = false;

        do {
            for (WorldServer server : mcServer.getWorlds()) {
//                used = server.getWorldProvider().getDimensionManager().getDimensionID() + 1 == dimension;

                if (used) { // getDimensionID() returns the dimension - 1
                    dimension++;
                    break;
                }
            }
        } while (used);

        World.Environment env = World.Environment.valueOf(world.getPropertyMap().getString(SlimeProperties.ENVIRONMENT).toUpperCase());

        DimensionManager dimensionManager = DimensionManager.a();
//        DimensionManager dimensionManager = DimensionManager.a(worldName, new DimensionManager(dimension, actualDimension.getSuffix(),
//                actualDimension, actualDimension.providerFactory::apply, actualDimension.hasSkyLight(), actualDimension
//                .getGenLayerZoomer(), actualDimension));

        CustomWorldServer server = null;
        try {
            Bukkit.broadcastMessage(ChatColor.of("#590c0c") + "Server-pre: " + server);
            Bukkit.broadcastMessage(ChatColor.of("#590c0c") + "Server-world: " + world);
            Bukkit.broadcastMessage(ChatColor.of("#590c0c") + "Server-DM: " + dimensionManager);
            Bukkit.broadcastMessage(ChatColor.of("#590c0c") + "Server-env: " + env);
            Bukkit.broadcastMessage(ChatColor.of("#590c0c") + "Server-CG: " + GeneratorSettings.a().getChunkGenerator());
            Bukkit.broadcastMessage(ChatColor.of("#590c0c") + "Server-WS: " + mcServer.D().worldDataServer);
            server = new CustomWorldServer((CraftSlimeWorld) world, dataManager, dimensionManager, env, GeneratorSettings.a().getChunkGenerator(), mcServer.D().worldDataServer, ResourceKey.a(MinecraftKey.a("OVERWORLD")), ResourceKey.a(MinecraftKey.a("OVERWORLD")), Arrays.asList(new MobSpawnerCat()), true, true);
            Bukkit.broadcastMessage(ChatColor.of("#590c0c") + "WORLD-NAME: " + server.getSlimeWorld().getName());
        } catch(IOException e) {
            Bukkit.broadcastMessage(ChatColor.of("#590c0c") + "Server-error: " + server);
            e.printStackTrace();
        }
        Bukkit.broadcastMessage(ChatColor.of("#590c0c") + "Server-post: " + server);

        LOGGER.info("Loading world " + worldName);
        long startTime = System.currentTimeMillis();

        server.setReady(true);
        mcServer.initWorld(server, mcServer.D().worldDataServer, null, GeneratorSettings.a().k());
//        mcServer.initWorld(server, dataManager.getWorldData(), new WorldSettings("", EnumGamemode.NOT_SET, true, EnumDifficulty.PEACEFUL, true, null, null), null);

        mcServer.server.addWorld(server.getWorld());
        mcServer.worldServer.put(server.getDimensionKey(), server);

        Bukkit.getPluginManager().callEvent(new WorldInitEvent(server.getWorld()));

        if (server.getWorld().getKeepSpawnInMemory()) {
            LOGGER.debug("Preparing start region for dimension '{}'/{}", worldName, dimensionManager.a(0));
            BlockPosition spawn = server.getSpawn();
            ChunkProviderServer provider = server.getChunkProvider();
            provider.addTicket(TicketType.START, new ChunkCoordIntPair(spawn), 11, Unit.INSTANCE);
        }

        Bukkit.getPluginManager().callEvent(new WorldLoadEvent(server.getWorld()));

        LOGGER.info("World " + worldName + " loaded in " + (System.currentTimeMillis() - startTime) + "ms.");
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
