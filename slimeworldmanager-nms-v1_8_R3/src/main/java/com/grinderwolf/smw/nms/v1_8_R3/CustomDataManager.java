package com.grinderwolf.smw.nms.v1_8_R3;

import com.grinderwolf.smw.api.SlimeWorld;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_8_R3.*;

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
        return new CustomChunkLoader(world);
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
