package com.grinderwolf.swm.nms.v1_14_R1;

import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import lombok.Getter;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.EnumDifficulty;
import net.minecraft.server.v1_14_R1.EnumGamemode;
import net.minecraft.server.v1_14_R1.WorldData;

@Getter
public class CustomWorldData extends WorldData {

    private final CraftSlimeWorld world;

    public CustomWorldData(CraftSlimeWorld world) {
        this.world = world;
        this.setGameType(EnumGamemode.NOT_SET);
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