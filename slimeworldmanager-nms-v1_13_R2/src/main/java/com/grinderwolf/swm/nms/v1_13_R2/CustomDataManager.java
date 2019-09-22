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
import net.minecraft.server.v1_13_R2.GameRules;
import net.minecraft.server.v1_13_R2.IChunkLoader;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.WorldData;
import net.minecraft.server.v1_13_R2.WorldNBTStorage;
import net.minecraft.server.v1_13_R2.WorldProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@Getter
public class CustomDataManager extends WorldNBTStorage {

    private static final GameRules EMPTY_GAMERULES = new GameRules();
    
    @Getter(value = AccessLevel.NONE)
    private final UUID uuid = UUID.randomUUID();
    private final SlimeWorld world;
    private final CustomChunkLoader chunkLoader;
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
        this.chunkLoader = new CustomChunkLoader((CraftSlimeWorld) world);
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
        return chunkLoader;
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
                String defaultValue = EMPTY_GAMERULES.get(rule).a();

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
