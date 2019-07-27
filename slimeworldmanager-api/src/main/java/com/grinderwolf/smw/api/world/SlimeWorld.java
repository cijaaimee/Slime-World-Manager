package com.grinderwolf.smw.api.world;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.smw.api.loaders.SlimeLoader;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

public interface SlimeWorld {

    public String getName();
    public SlimeLoader getLoader();
    public SlimeChunk getChunk(int x, int z);
    public CompoundTag getExtraData();
    public SlimeProperties getProperties();

    @Getter
    @Builder(toBuilder = true)
    public class SlimeProperties {

        double spawnX;
        double spawnY;
        double spawnZ;

        int difficulty;

        @Accessors(fluent = true)
        boolean allowMonsters;
        @Accessors(fluent = true)
        boolean allowAnimals;
    }
}
