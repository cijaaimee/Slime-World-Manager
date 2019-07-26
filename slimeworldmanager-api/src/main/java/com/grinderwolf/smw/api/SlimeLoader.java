package com.grinderwolf.smw.api;

import com.grinderwolf.smw.api.exceptions.CorruptedWorldException;
import com.grinderwolf.smw.api.exceptions.UnknownWorldException;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface SlimeLoader {

    public SlimeWorld loadWorld(String worldName) throws UnknownWorldException, IOException, CorruptedWorldException;
    public boolean worldExists(String worldName);
    public void saveWorld(SlimeWorld world) throws IOException;
}
