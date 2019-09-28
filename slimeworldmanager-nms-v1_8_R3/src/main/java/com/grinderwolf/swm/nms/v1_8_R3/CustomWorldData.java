package com.grinderwolf.swm.nms.v1_8_R3;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.WorldData;
import net.minecraft.server.v1_8_R3.WorldSettings;

import java.util.Optional;

@Getter
public class CustomWorldData extends WorldData {

    private final CraftSlimeWorld world;

    CustomWorldData(CraftSlimeWorld world) {
        this.world = world;
        this.setGameType(WorldSettings.EnumGamemode.NOT_SET);

        CompoundTag extraData = world.getExtraData();
        Optional<CompoundTag> gameRules = extraData.getAsCompoundTag("gamerules");
        gameRules.ifPresent(compoundTag -> this.x().a((NBTTagCompound) Converter.convertTag(compoundTag)));
    }

    @Override
    public String getName() {
        return world.getName();
    }

}
