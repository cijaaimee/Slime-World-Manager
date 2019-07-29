package com.grinderwolf.smw.api.loaders;

import com.grinderwolf.smw.api.exceptions.UnknownWorldException;
import com.grinderwolf.smw.api.exceptions.WorldInUseException;

import java.io.IOException;

/**
 * SlimeLoaders are in charge of loading worlds
 * from a data source, and also locking and
 * deleting them.
 */
public interface SlimeLoader {

    /**
     * Load a world's data file. In case {@code readOnly} is false,
     * the world automatically gets locked so no other server
     * can access it in write-mode.
     *
     * @param worldName The name of the world.
     * @param readOnly If false, a {@link WorldInUseException} should be thrown when the world is locked.
     *
     * @return The world's data file, contained inside a byte array.
     *
     * @throws UnknownWorldException if the world cannot be found.
     * @throws WorldInUseException if the world is locked
     * @throws IOException if the world could not be obtained.
     */
    public byte[] loadWorld(String worldName, boolean readOnly) throws UnknownWorldException, WorldInUseException, IOException;

    /**
     * Checks whether or not a world exists
     * inside the data source.
     *
     * @param worldName The name of the world.
     *
     * @return <code>true</code> if the world exists inside the data source, <code>false</code> otherwhise.
     *
     * @throws IOException if the world could not be obtained.
     */
    public boolean worldExists(String worldName) throws IOException;

    /**
     * Saves the world's data file. This method will also
     * lock the world, in case it's not locked already.
     *
     * @param worldName The name of the world.
     * @param serializedWorld The world's data file, contained inside a byte array.
     *
     * @throws IOException if the world could not be saved.
     */
    public void saveWorld(String worldName, byte[] serializedWorld) throws IOException;

    /**
     * Manually unlocks a world.
     *
     * @param worldName The name of the world.
     *
     * @throws IOException if the world could not be unlocked.
     */
    public void unlockWorld(String worldName) throws IOException;

    /**
     * Checks whether or not a world is locked.
     *
     * @param worldName The name of the world.
     *
     * @return <code>true</code> if the world is locked, <code>false</code> otherwhise.
     *
     * @throws UnknownWorldException if the world could not be found.
     * @throws IOException if the world could not be obtained.
     */
    public boolean isWorldLocked(String worldName) throws UnknownWorldException, IOException;

    /**
     * Deletes a world from the data source.
     *
     * @param worldName name of the world
     *
     * @throws UnknownWorldException if the world could not be found.
     * @throws IOException if the world could not be deleted.
     */
    public void deleteWorld(String worldName) throws UnknownWorldException, IOException;
}
