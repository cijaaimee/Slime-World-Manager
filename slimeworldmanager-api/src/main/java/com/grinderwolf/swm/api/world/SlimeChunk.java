package com.grinderwolf.swm.api.world;

import com.flowpowered.nbt.CompoundTag;

import java.util.List;

/**
 * In-memory representation of a SRF chunk.
 */
public interface SlimeChunk {

    /**
     * Returns the X coordinate of the chunk.
     *
     * @return X coordinate of the chunk.
     */
    public int getX();

    /**
     * Returns the Z coordinate of the chunk.
     *
     * @return Z coordinate of the chunk.
     */
    public int getZ();

    /**
     * Returns all the sections of the chunk.
     *
     * @return A {@link SlimeChunkSection} array.
     */
    public SlimeChunkSection[] getSections();

    /**
     * Returns the height maps of the chunk. If it's a pre 1.13 world,
     * a {@link com.flowpowered.nbt.IntArrayTag} containing the height
     * map will be stored inside here by the name of 'heightMap'.
     *
     * @return A {@link CompoundTag} containing all the height maps of the chunk.
     */
    public CompoundTag getHeightMaps();

    /**
     * Returns all the biomes of the chunk. In case it's a pre 1.13 world,
     * every <code>int</code> inside the array will contain two biomes,
     * and should be converted into a <code>byte[]</code>.
     *
     * @return A <code>int[]</code> containing all the biomes of the chunk.
     */
    public int[] getBiomes();

    /**
     * Returns all the tile entities of the chunk.
     *
     * @return A {@link CompoundTag} containing all the tile entities of the chunk.
     */
    public List<CompoundTag> getTileEntities();

    /**
     * Returns all the entities of the chunk.
     *
     * @return A {@link CompoundTag} containing all the entities of the chunk.
     */
    public List<CompoundTag> getEntities();
}
