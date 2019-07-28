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

        final double spawnX;
        final double spawnY;
        final double spawnZ;

        final int difficulty;

        @Accessors(fluent = true)
        final boolean allowMonsters;
        @Accessors(fluent = true)
        final boolean allowAnimals;
        final boolean readOnly;
    }
}
