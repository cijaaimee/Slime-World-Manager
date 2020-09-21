package com.grinderwolf.swm.nms.v1_16_R1;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.mojang.serialization.Lifecycle;
import net.minecraft.server.v1_16_R1.*;
import lombok.Getter;

import java.util.Objects;
import java.util.Properties;

@Getter
public class CustomNBTStorage extends WorldNBTStorage {

    private static final GameRules EMPTY_GAMERULES = new GameRules();

    private final SlimeWorld world;
    private WorldData worldData;

    public CustomNBTStorage(SlimeWorld world, Convertable.ConversionSession conversionSession) {
        super(conversionSession, MinecraftServer.getServer().getDataFixer());

        this.world = world;
    }

    public WorldData getWorldData() {
        if (worldData == null) {
            SlimePropertyMap propertyMap = world.getPropertyMap();
            Properties properties = new Properties();
            String defaultBiome = propertyMap.getString(SlimeProperties.DEFAULT_BIOME);
            String generatorString = "{\"structures\":{\"structures\":{}},\"biome\":\"" + defaultBiome + "\",\"layers\":[]}";

            properties.put("generator-settings", generatorString);
            properties.put("level-type", "FLAT");

            GeneratorSettings generatorSettings = GeneratorSettings.a(properties);

            worldData = new CustomWorldData((CraftSlimeWorld) world, generatorSettings);
        }

        return worldData;
    }

    public void saveWorldData(WorldData worldData) {
        CompoundTag gameRules = (CompoundTag) Converter.convertTag("gamerules", worldData.p().a()).getAsCompoundTag().get();
        CompoundTag extraData = this.world.getExtraData();

        extraData.getValue().remove("gamerules");

        if (!gameRules.getValue().isEmpty()) {
            extraData.getValue().put("gamerules", gameRules);
        }
    }

    @Override public void save(EntityHuman entityHuman) {}
    @Override public NBTTagCompound load(EntityHuman entityHuman) { return null; }
    @Override public String[] getSeenPlayers() { return new String[0]; }
}