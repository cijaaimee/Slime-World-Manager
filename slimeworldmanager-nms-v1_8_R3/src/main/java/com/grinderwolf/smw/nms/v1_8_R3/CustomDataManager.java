package com.grinderwolf.smw.nms.v1_8_R3;

import com.grinderwolf.smw.api.world.SlimeWorld;
import com.grinderwolf.smw.nms.CraftSlimeWorld;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_8_R3.IChunkLoader;
import net.minecraft.server.v1_8_R3.IDataManager;
import net.minecraft.server.v1_8_R3.IPlayerFileData;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.WorldData;
import net.minecraft.server.v1_8_R3.WorldProvider;

import java.io.File;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class CustomDataManager implements IDataManager {

    @Getter(value = AccessLevel.NONE)
    private final UUID uuid = UUID.randomUUID();
    private final IPlayerFileData playerFileData = new EmptyPlayerFileData();
    private final SlimeWorld world;
    private WorldData worldData;

    @Override
    public WorldData getWorldData() {
        if (worldData == null) {
            worldData = new CustomWorldData(world.getName());
        }

        return worldData;
    }

    @Override public void checkSession() { }

    @Override
    public IChunkLoader createChunkLoader(WorldProvider worldProvider) {
        return new CustomChunkLoader((CraftSlimeWorld) world);
    }

    @Override
    public void saveWorldData(WorldData worldData, NBTTagCompound nbtTagCompound) { }

    @Override
    public void saveWorldData(WorldData worldData) { }

    @Override
    public void a() {

    }

    @Override
    public File getDirectory() {
        return null;
    }

    @Override
    public File getDataFile(String s) {
        return null;
    }

    @Override
    public String g() {
        return null;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }
}
