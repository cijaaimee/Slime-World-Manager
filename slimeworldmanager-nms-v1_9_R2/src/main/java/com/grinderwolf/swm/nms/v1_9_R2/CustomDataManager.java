package com.grinderwolf.swm.nms.v1_9_R2;

import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.server.v1_9_R2.EntityHuman;
import net.minecraft.server.v1_9_R2.NBTTagCompound;
import net.minecraft.server.v1_9_R2.WorldData;
import net.minecraft.server.v1_9_R2.WorldNBTStorage;

import java.io.File;
import java.util.UUID;

@Getter
public class CustomDataManager extends WorldNBTStorage {

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
    public CustomDataManager(SlimeWorld world) {
        super(new File("temp_" + world.getName()), world.getName(), false, null);

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

    @Override public void checkSession() { }

    @Override
    public void saveWorldData(WorldData worldData, NBTTagCompound nbtTagCompound) { }

    @Override
    public void saveWorldData(WorldData worldData) { }

    @Override
    public void a() {

    }

    @Override
    public File getDataFile(String s) {
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
