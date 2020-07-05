package com.grinderwolf.swm.nms.v1_16_R1;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.grinderwolf.swm.nms.SlimeNMS;
import lombok.Getter;
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

        CustomNBTStorage dataManager = null;
        Convertable.ConversionSession conversionSession = null;
        try {
            conversionSession = Convertable.a(new File(world.getName()).toPath()).c("", WorldDimension.OVERWORLD);
        } catch(IOException e) {
            e.printStackTrace();
        }
        try {
            dataManager = new CustomNBTStorage(world, conversionSession);
        } catch(IOException e) {
            e.printStackTrace();
        }
        MinecraftServer mcServer = MinecraftServer.getServer();

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

//        DimensionManager actualDimension = DimensionManager.a(env.getId());
//        DimensionManager dimensionManager = DimensionManager.register(worldName, new DimensionManager(dimension, actualDimension.getSuffix(),
//                actualDimension.folder, actualDimension.providerFactory::apply, actualDimension.hasSkyLight(), actualDimension
//                .getGenLayerZoomer(), actualDimension));

        CustomWorldServer server = null;
        try {
            server = new CustomWorldServer((CraftSlimeWorld) world, dataManager, DimensionManager.a(), env, conversionSession);
        } catch(IOException e) {
            e.printStackTrace();
        }

        LOGGER.info("Loading world " + worldName);
        long startTime = System.currentTimeMillis();

        server.setReady(true);
        mcServer.initWorld(server, server.worldDataServer, null, GeneratorSettings.a());
//        mcServer.initWorld(server, dataManager.getWorldData(), new WorldSettings("", EnumGamemode.NOT_SET, true, EnumDifficulty.PEACEFUL, true, null, null), null);

        mcServer.server.addWorld(server.getWorld());
//        mcServer.worldServer.put(dimensionManager, server);

        Bukkit.getPluginManager().callEvent(new WorldInitEvent(server.getWorld()));

        if (server.getWorld().getKeepSpawnInMemory()) {
//            LOGGER.debug("Preparing start region for dimension '{}'/{}", worldName, DimensionManager.a(0));
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
