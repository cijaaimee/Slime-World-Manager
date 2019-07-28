package com.grinderwolf.smw.api.loaders;

import com.grinderwolf.smw.api.exceptions.UnknownWorldException;
import com.grinderwolf.smw.api.exceptions.WorldInUseException;

import java.io.IOException;

public interface SlimeLoader {

    public byte[] loadWorld(String worldName, boolean readOnly) throws UnknownWorldException, WorldInUseException, IOException;
    public boolean worldExists(String worldName) throws IOException;
    public void saveWorld(String worldName, byte[] serializedWorld) throws IOException;
    public void unlockWorld(String worldName) throws IOException;
    public boolean isWorldLocked(String worldName) throws IOException, UnknownWorldException;
    public void deleteWorld(String worldName) throws IOException, UnknownWorldException;
}
