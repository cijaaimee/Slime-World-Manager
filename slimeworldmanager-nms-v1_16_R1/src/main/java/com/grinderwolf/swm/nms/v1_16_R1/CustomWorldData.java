package com.grinderwolf.swm.nms.v1_16_R1;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.mojang.serialization.DynamicLike;
import com.mojang.serialization.Lifecycle;
import lombok.Getter;
import net.minecraft.server.v1_16_R1.*;
import net.minecraft.server.v1_16_R1.GameRules.GameRuleInt;
import net.minecraft.server.v1_16_R1.GameRules.GameRuleKey;
import net.minecraft.server.v1_16_R1.GameRules.GameRuleValue;
import org.bukkit.Bukkit;
import org.bukkit.WorldType;
import org.bukkit.craftbukkit.v1_16_R1.CraftServer;
import org.bukkit.craftbukkit.v1_16_R1.CraftWorld;

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
            EnumDifficulty.NORMAL,
            true,
            new GameRules(),
            MinecraftServer.getServer().datapackconfiguration),
            ((DedicatedServer)MinecraftServer.getServer()).getDedicatedServerProperties().generatorSettings,
            Lifecycle.stable()
        );

        checkName(world.getName());

        this.world = world;
        this.type = WorldType.getByName(world.getPropertyMap().getString(SlimeProperties.WORLD_TYPE).toUpperCase());
        this.setGameType(EnumGamemode.NOT_SET);

        // Game rules
        CompoundTag extraData = world.getExtraData();
        Optional<CompoundTag> gameRules = extraData.getAsCompoundTag("gamerules");

        gameRules.ifPresent(compoundTag -> {
            NBTTagCompound compound = (NBTTagCompound) Converter.convertTag(compoundTag);
            Map<String, GameRuleKey<?>> gameRuleKeys = CraftWorld.getGameRulesNMS();
            MinecraftServer mcServer = MinecraftServer.getServer();

            compound.getKeys().forEach(gameRule -> {
                if(gameRuleKeys.containsKey(gameRule)) {
                    GameRuleValue<?> gameRuleValue = this.p().get(gameRuleKeys.get(gameRule));
                    String theValue = compound.getString(gameRule);
                    gameRuleValue.setValue(theValue);
                    gameRuleValue.onChange(mcServer);
                }
            });
        });
    }

    public String getName() {
        return world.getName();
    }

    public boolean d() {
        return true;
    }

}