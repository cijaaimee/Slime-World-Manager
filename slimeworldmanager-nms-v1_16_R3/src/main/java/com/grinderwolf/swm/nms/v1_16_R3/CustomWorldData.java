package com.grinderwolf.swm.nms.v1_16_R3;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.mojang.serialization.Lifecycle;
import lombok.Getter;

import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.DedicatedServer;
import net.minecraft.server.v1_16_R3.EnumDifficulty;
import net.minecraft.server.v1_16_R3.EnumGamemode;
import net.minecraft.server.v1_16_R3.GameRules;
import net.minecraft.server.v1_16_R3.GameRules.GameRuleKey;
import net.minecraft.server.v1_16_R3.GameRules.GameRuleValue;
import net.minecraft.server.v1_16_R3.MinecraftServer;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.WorldDataServer;
import net.minecraft.server.v1_16_R3.WorldSettings;
import org.bukkit.WorldType;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;

import java.util.Map;
import java.util.Optional;

@Getter
public class CustomWorldData extends WorldDataServer {

    private final CraftSlimeWorld world;
    private final WorldType type;

    CustomWorldData(CraftSlimeWorld world) {
        super(new WorldSettings(
                world.getName(),
                EnumGamemode.NOT_SET,
                false,
                EnumDifficulty.valueOf(world.getPropertyMap().getString(SlimeProperties.DIFFICULTY).toUpperCase()),
                true,
                new GameRules(),
                MinecraftServer.getServer().datapackconfiguration
            ),
            ((DedicatedServer)MinecraftServer.getServer()).getDedicatedServerProperties().generatorSettings,
            Lifecycle.stable()
        );

        checkName(world.getName());

        this.world = world;
        this.type = WorldType.getByName(world.getPropertyMap().getString(SlimeProperties.WORLD_TYPE).toUpperCase());
        this.setGameType(EnumGamemode.NOT_SET);
        BlockPosition spawn = new BlockPosition(world.getPropertyMap().getInt(SlimeProperties.SPAWN_X), world.getPropertyMap().getInt(SlimeProperties.SPAWN_Y), world.getPropertyMap().getInt(SlimeProperties.SPAWN_Z));
        this.setSpawn(spawn, 1);

        // Game rules
        CompoundTag extraData = world.getExtraData();
        Optional<CompoundTag> gameRules = extraData.getAsCompoundTag("gamerules");

        gameRules.ifPresent(compoundTag -> {
            NBTTagCompound compound = (NBTTagCompound) Converter.convertTag(compoundTag);
            Map<String, GameRuleKey<?>> gameRuleKeys = CraftWorld.getGameRulesNMS();
            MinecraftServer mcServer = MinecraftServer.getServer();

            compound.getKeys().forEach(gameRule -> {
                if(gameRuleKeys.containsKey(gameRule)) {
                    GameRuleValue<?> gameRuleValue = this.q().get(gameRuleKeys.get(gameRule));
                    String theValue = compound.getString(gameRule);
                    gameRuleValue.setValue(theValue);
                    gameRuleValue.onChange(mcServer);
                }
            });
        });
    }

    @Override
    public String getName() {
        return world.getName();
    }

    public boolean o() {
        return true;
    }

}