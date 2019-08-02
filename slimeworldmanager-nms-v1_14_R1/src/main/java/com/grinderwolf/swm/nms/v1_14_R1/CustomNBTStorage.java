package com.grinderwolf.swm.nms.v1_14_R1;

import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.server.v1_14_R1.EntityHuman;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.WorldData;
import net.minecraft.server.v1_14_R1.WorldNBTStorage;

import java.io.File;
import java.util.UUID;

@Getter
public class CustomNBTStorage extends WorldNBTStorage {

    @Getter(value = AccessLevel.NONE)
    private final UUID uuid = UUID.randomUUID();
    private final SlimeWorld world;
    private WorldData worldData;

    public CustomNBTStorage(SlimeWorld world) {
        super(new File("temp_" + world.getName()), world.getName(), null, null);

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