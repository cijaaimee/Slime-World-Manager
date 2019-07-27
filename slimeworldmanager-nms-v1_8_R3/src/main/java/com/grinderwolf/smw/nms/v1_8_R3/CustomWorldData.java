package com.grinderwolf.smw.nms.v1_8_R3;

import com.grinderwolf.smw.api.world.SlimeWorld;
import com.grinderwolf.smw.nms.CraftSlimeWorld;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EnumDifficulty;
import net.minecraft.server.v1_8_R3.WorldData;

@Getter
@RequiredArgsConstructor
public class CustomWorldData extends WorldData {

    private final CraftSlimeWorld world;

    @Override
    public String getName() {
        return world.getName();
    }

    @Override
    public void setSpawn(BlockPosition position) {
        super.setSpawn(position);

        // Keep properties updated
        SlimeWorld.SlimeProperties newProps = world.getProperties().toBuilder().spawnX(position.getX()).spawnY(position.getY()).spawnZ(position.getZ()).build();
        world.setProperties(newProps);
    }

    @Override
    public void setDifficulty(EnumDifficulty difficulty) {
        super.setDifficulty(difficulty);

        // Keep properties updated
        SlimeWorld.SlimeProperties newProps = world.getProperties().toBuilder().difficulty(difficulty.a()).build();
        world.setProperties(newProps);
    }
}
