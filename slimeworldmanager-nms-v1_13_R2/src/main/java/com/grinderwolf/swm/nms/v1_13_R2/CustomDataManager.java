package com.grinderwolf.swm.nms.v1_13_R2;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.server.v1_13_R2.DimensionManager;
import net.minecraft.server.v1_13_R2.EntityHuman;
import net.minecraft.server.v1_13_R2.IChunkLoader;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.WorldData;
import net.minecraft.server.v1_13_R2.WorldNBTStorage;
import net.minecraft.server.v1_13_R2.WorldProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class CustomDataManager extends WorldNBTStorage {

    private static final Map<String, String> defaultValues = new HashMap<>();

    // We cannot get the default values automatically because the method
    // return type is paperspigot is not the same that the one in spigot,
    // so there's no easy way to support both at the same time
    static {
        defaultValues.put("doFireTick", "true");
        defaultValues.put("mobGriefing", "true");
        defaultValues.put("keepInventory", "false");
        defaultValues.put("doMobSpawning", "true");
        defaultValues.put("doMobLoot", "true");
        defaultValues.put("doTileDrops", "true");
        defaultValues.put("doEntityDrops", "true");
        defaultValues.put("commandBlockOutput", "true");
        defaultValues.put("naturalRegeneration", "true");
        defaultValues.put("doDaylightCycle", "true");
        defaultValues.put("logAdminCommands", "true");
        defaultValues.put("showDeathMessages", "true");
        defaultValues.put("randomTickSpeed", "3");
        defaultValues.put("sendCommandFeedback", "true");
        defaultValues.put("reducedDebugInfo", "false");
        defaultValues.put("spectatorsGenerateChunks", "true");
        defaultValues.put("spawnRadius", "10");
        defaultValues.put("disableElytraMovementCheck", "false");
        defaultValues.put("maxEntityCramming", "24");
        defaultValues.put("doWeatherCycle", "true");
        defaultValues.put("doLimitedCrafting", "false");
        defaultValues.put("maxCommandChainLength", "65536");
        defaultValues.put("announceAdvancements", "true");
    }
    
    @Getter(value = AccessLevel.NONE)
    private final UUID uuid = UUID.randomUUID();
    private final SlimeWorld world;
    private WorldData worldData;

    // When unloading a world, Spigot tries to remove the region file from its cache.
    // To do so, it casts the world's IDataManager to a WorldNBTStorage, to be able
    // to use the getDirectory() method. Thanks to this, we have to create a custom
    // WorldNBTStorage with a fake file instead of just implementing the IDataManager interface
    //
    // Thanks Spigot!
    CustomDataManager(SlimeWorld world) {
        super(new File("temp_" + world.getName()), world.getName(), null, null);

        // The WorldNBTStorage automatically creates some files inside the base dir, so we have to delete them
        // (Thanks again Spigot)

        // Can't just access the baseDir field inside WorldNBTStorage cause it's private :P
        File baseDir = new File("temp_" + world.getName(), world.getName());
        new File(baseDir, "session.lock").delete();
        new File(baseDir, "data").delete();

        baseDir.delete();
        baseDir.getParentFile().delete();

        this.world = world;
    }

    @Override
    public WorldData getWorldData() {
        if (worldData == null) {
            worldData = new CustomWorldData((CraftSlimeWorld) world);
        }

        return worldData;
    }

    @Override
    public IChunkLoader createChunkLoader(WorldProvider worldProvider) {
        return new CustomChunkLoader((CraftSlimeWorld) world);
    }

    @Override public void checkSession() { }

    @Override
    public void saveWorldData(WorldData worldData, NBTTagCompound nbtTagCompound) {
        CompoundTag gameRules = (CompoundTag) Converter.convertTag("gamerules", worldData.w().a()).getAsCompoundTag().get();
        CompoundTag extraData = this.world.getExtraData();

        extraData.getValue().remove("gamerules");

        if (!gameRules.getValue().isEmpty()) {
            // Remove default values to save space
            for (Map.Entry<String, Tag<?>> entry : new ArrayList<>(gameRules.getValue().entrySet())) {
                String rule = entry.getKey();
                StringTag valueTag = (StringTag) entry.getValue();
                String defaultValue = defaultValues.get(rule);

                if (defaultValue != null && defaultValue.equalsIgnoreCase(valueTag.getValue())) {
                    gameRules.getValue().remove(rule);
                }
            }

            // Maybe all the gamerules stored were the default values
            if (!gameRules.getValue().isEmpty()) {
                extraData.getValue().put("gamerules", gameRules);
            }
        }
    }

    @Override
    public void saveWorldData(WorldData worldData) {
        this.saveWorldData(worldData, null);
    }

    @Override
    public void a() {

    }

    @Override
    public File getDataFile(DimensionManager manager, String s) {
        return null;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public void save(EntityHuman entityHuman) {

    }

    @Override
    public NBTTagCompound load(EntityHuman entityHuman) {
        return null;
    }

    @Override public String[] getSeenPlayers() {
        return new String[0];
    }
}
