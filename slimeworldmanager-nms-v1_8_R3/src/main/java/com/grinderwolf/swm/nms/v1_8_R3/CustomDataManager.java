/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.nms.v1_8_R3;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class CustomDataManager extends WorldNBTStorage {

    private static final Map<String, String> defaultValues;

    static {
        GameRules emptyRules = new GameRules();
        String[] rules = emptyRules.getGameRules();

        defaultValues =
                Arrays.stream(rules).collect(Collectors.toMap((rule) -> rule, emptyRules::get));
    }

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
        super(new File("temp_" + world.getName()), world.getName(), false);

        // The WorldNBTStorage automatically creates some files inside the base dir, so we have to
        // delete them
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
    public void checkSession() {}

    @Override
    public IChunkLoader createChunkLoader(WorldProvider worldProvider) {
        return chunkLoader;
    }

    @Override
    public void saveWorldData(WorldData worldData, NBTTagCompound nbtTagCompound) {
        CompoundTag gameRules =
                (CompoundTag)
                        Converter.convertTag("gamerules", worldData.x().a())
                                .getAsCompoundTag()
                                .get();
        CompoundTag extraData = this.world.getExtraData();

        extraData.getValue().remove("gamerules");

        if (!gameRules.getValue().isEmpty()) {
            // Remove default values to save space
            for (Map.Entry<String, Tag<?>> entry :
                    new ArrayList<>(gameRules.getValue().entrySet())) {
                String rule = entry.getKey();
                StringTag valueTag = (StringTag) entry.getValue();
                String defaultValue = defaultValues.get(rule);

                if (valueTag.getValue().equalsIgnoreCase(defaultValue)) {
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
    public void a() {}

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

    @Override
    public void save(EntityHuman entityHuman) {}

    @Override
    public NBTTagCompound load(EntityHuman entityHuman) {
        return null;
    }

    @Override
    public String[] getSeenPlayers() {
        return new String[0];
    }
}
