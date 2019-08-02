package com.grinderwolf.swm.api.world;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * In-memory representation of a SRF world.
 */
public interface SlimeWorld {

    /**
     * Returns the name of the world.
     *
     * @return The name of the world.
     */
    public String getName();

    /**
     * Returns the {@link SlimeLoader} used
     * to load and store the world.
     *
     * @return The {@link SlimeLoader} used to load and store the world.
     */
    public SlimeLoader getLoader();

    /**
     * Returns the chunk that belongs to the coordinates specified.
     *
     * @param x X coordinate.
     * @param z Z coordinate.
     *
     * @return The {@link SlimeChunk} that belongs to those coordinates.
     */
    public SlimeChunk getChunk(int x, int z);

    /**
     * Returns the extra data of the world. Inside this {@link CompoundTag}
     * can be stored any information, to then be retrieved later, as it's
     * saved alongside the world data.
     *
     * @return A {@link CompoundTag} containing the extra data of the world.
     */
    public CompoundTag getExtraData();

    /**
     * Returns the properties of the world. These properties are automatically
     * kept up-to-date when the world is loaded and its properties are updated.
     *
     * @return A {@link SlimeProperties} object with all the current properties of the world.
     */
    public SlimeProperties getProperties();

    /**
     * All the currently-available properties of the world.
     */
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
