package com.grinderwolf.swm.nms.v1_13_R2;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.grinderwolf.swm.nms.SlimeNMS;
import com.mojang.datafixers.DataFixTypes;
import lombok.Getter;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.DataConverterRegistry;
import net.minecraft.server.v1_13_R2.DimensionManager;
import net.minecraft.server.v1_13_R2.GameProfileSerializer;
import net.minecraft.server.v1_13_R2.MinecraftServer;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.WorldServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;

@Getter
public class v1_13_R2SlimeNMS implements SlimeNMS {

    private static final Logger LOGGER = LogManager.getLogger("SWM");
    public static final boolean IS_PAPER;

    static {
        boolean paper = true;

        try {
           Class.forName("com.destroystokyo.paper.PaperWorldConfig");
        } catch (ClassNotFoundException e) {
            paper = false;
        }

        IS_PAPER = paper;
    }

    private final byte worldVersion = 0x04;

    private WorldServer defaultWorld;
    private WorldServer defaultNetherWorld;
    private WorldServer defaultEndWorld;

    public v1_13_R2SlimeNMS() {
        try {
            CraftCLSMBridge.initialize(this);
        }  catch (NoClassDefFoundError ex) {
            if (IS_PAPER) {
                LOGGER.error("Failed to find ClassModifier classes. Are you sure you installed it correctly?");
                System.exit(1); // No ClassModifier, no party
            }

            LOGGER.warn("Failed to find ClassModifier classes. Overriding default worlds is disabled.");
        }
    }

    @Override
    public void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld) {
        if (normalWorld != null) {
            defaultWorld = new CustomWorldServer((CraftSlimeWorld) normalWorld, new CustomDataManager(normalWorld), DimensionManager.OVERWORLD);
        }

        if (netherWorld != null) {
            defaultNetherWorld = new CustomWorldServer((CraftSlimeWorld) netherWorld, new CustomDataManager(netherWorld), DimensionManager.NETHER);
        }

        if (endWorld != null) {
            defaultEndWorld = new CustomWorldServer((CraftSlimeWorld) endWorld, new CustomDataManager(endWorld), DimensionManager.THE_END);
        }
    }

    @Override
    public Object createNMSWorld(SlimeWorld world) {
        CustomDataManager dataManager = new CustomDataManager(world);
        MinecraftServer mcServer = MinecraftServer.getServer();

        int dimension = CraftWorld.CUSTOM_DIMENSION_OFFSET + mcServer.worldServer.size();

        for (WorldServer server : mcServer.getWorlds()) {
            if (server.dimension.getDimensionID() + 1 == dimension) { // getDimensionID() returns the dimension - 1
                dimension++;
            }
        }

        String worldName = world.getName();
        DimensionManager dimensionManager = new DimensionManager(dimension, worldName, worldName, DimensionManager.OVERWORLD::e);
        WorldServer server = new CustomWorldServer((CraftSlimeWorld) world, dataManager, dimensionManager);

        if (server.getWorld().getKeepSpawnInMemory()) {
            LOGGER.debug("Preparing start region for world " + worldName);

            long timeMillis = System.currentTimeMillis();

            for (int x = -196; x <= 196; x += 16) {
                for (int z = -196; z <= 196; z += 16) {
                    long currentTime = System.currentTimeMillis();

                    if (currentTime > timeMillis + 1000L) {
                        int total = (196 * 2 + 1) * (196 * 2 + 1);
                        int done = (x + 196) * (196 * 2 + 1) + z + 1;

                        LOGGER.debug("Preparing spawn area for " + worldName + ": " + (done * 100 / total) + "%");
                        timeMillis = currentTime;
                    }

                    BlockPosition spawn = server.getSpawn();
                    server.getChunkProvider().getChunkAt(spawn.getX() + x >> 4, spawn.getZ() + z >> 4, true, true);
                }
            }
        }

        return server;
    }

    @Override
    public void generateWorld(SlimeWorld world) {
        addWorldToServerList(createNMSWorld(world));
    }

    @Override
    public void addWorldToServerList(Object worldObject) {
        if (!(worldObject instanceof WorldServer)) {
            throw new IllegalArgumentException("World object must be an instance of WorldServer!");
        }

        CustomWorldServer server = (CustomWorldServer) worldObject;
        String worldName = server.getWorldData().getName();

        if (Bukkit.getWorld(worldName) != null) {
            throw new IllegalArgumentException("World " + worldName + " already exists! Maybe it's an outdated SlimeWorld object?");
        }

        LOGGER.info("Loading world " + worldName);
        long startTime = System.currentTimeMillis();

        server.setReady(true);
        MinecraftServer mcServer = MinecraftServer.getServer();

        mcServer.server.addWorld(server.getWorld());
        MinecraftServer.getServer().worldServer.put(server.dimension, server);

        Bukkit.getPluginManager().callEvent(new WorldInitEvent(server.getWorld()));
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
