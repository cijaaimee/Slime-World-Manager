package com.grinderwolf.swm.nms.v1_9_R2;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import lombok.Getter;
import net.minecraft.server.v1_9_R2.NBTTagCompound;
import net.minecraft.server.v1_9_R2.WorldData;
import net.minecraft.server.v1_9_R2.WorldSettings;

import java.util.Optional;

@Getter
public class CustomWorldData extends WorldData {

    private final CraftSlimeWorld world;

    CustomWorldData(CraftSlimeWorld world) {
        this.world = world;
        this.setGameType(WorldSettings.EnumGamemode.NOT_SET);

        // Game rules
        CompoundTag extraData = world.getExtraData();
        Optional<CompoundTag> gameRules = extraData.getAsCompoundTag("gamerules");
        gameRules.ifPresent(compoundTag -> this.w().a((NBTTagCompound) Converter.convertTag(compoundTag)));
    }

    @Override
    public String getName() {
        return world.getName();
    }

}
