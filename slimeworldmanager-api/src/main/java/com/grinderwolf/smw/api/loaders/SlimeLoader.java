package com.grinderwolf.smw.api.loaders;

import com.grinderwolf.smw.api.exceptions.UnknownWorldException;

import java.io.IOException;

public interface SlimeLoader {

    public byte[] loadWorld(String worldName) throws UnknownWorldException, IOException;
    public boolean worldExists(String worldName);
    public void saveWorld(String worldName, byte[] serializedWorld) throws IOException;
}
