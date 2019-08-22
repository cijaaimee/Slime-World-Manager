package com.grinderwolf.swm.nms.v1_12_R1;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import lombok.Getter;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.EnumDifficulty;
import net.minecraft.server.v1_12_R1.EnumGamemode;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.WorldData;

import java.util.Optional;

@Getter
public class CustomWorldData extends WorldData {

    private final CraftSlimeWorld world;

    CustomWorldData(CraftSlimeWorld world) {
        this.world = world;
        this.setGameType(EnumGamemode.NOT_SET);

        // Game rules
        CompoundTag extraData = world.getExtraData();
        Optional<CompoundTag> gameRules = extraData.getAsCompoundTag("gamerules");
        gameRules.ifPresent(compoundTag -> this.w().a((NBTTagCompound) Converter.convertTag(compoundTag)));
    }

    @Override
    public String getName() {
        return world.getName();
    }

    @Override
    public void setSpawn(BlockPosition position) {
        super.setSpawn(position);

        // Keep properties updated
        SlimeWorld.SlimeProperties oldProps = world.getProperties();

        if (oldProps.getSpawnX() != position.getX() || oldProps.getSpawnY() != position.getY() || oldProps.getSpawnZ() != position.getZ()) {
            SlimeWorld.SlimeProperties newProps = oldProps.toBuilder().spawnX(position.getX()).spawnY(position.getY()).spawnZ(position.getZ()).build();
            world.setProperties(newProps);
        }
    }

    @Override
    public void setDifficulty(EnumDifficulty difficulty) {
        super.setDifficulty(difficulty);

        // Keep properties updated
        SlimeWorld.SlimeProperties oldProps = world.getProperties();

        if (oldProps.getDifficulty() != difficulty.a()) {
            SlimeWorld.SlimeProperties newProps = oldProps.toBuilder().difficulty(difficulty.a()).build();
            world.setProperties(newProps);
        }
    }
}
