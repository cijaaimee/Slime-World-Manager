package com.grinderwolf.swm.nms.v1_16_R1;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.mojang.serialization.Lifecycle;
import lombok.Getter;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.WorldType;

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
        gameRules.ifPresent(compoundTag -> this.a((NBTTagCompound) Converter.convertTag(compoundTag)));
    }

    public String getName() {
        return world.getName();
    }

    public boolean d() {
        return true;
    }

}