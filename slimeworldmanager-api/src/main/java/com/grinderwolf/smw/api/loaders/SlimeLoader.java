package com.grinderwolf.smw.api.loaders;

import com.grinderwolf.smw.api.exceptions.CorruptedWorldException;
import com.grinderwolf.smw.api.exceptions.UnknownWorldException;
import com.grinderwolf.smw.api.world.SlimeWorld;

import java.io.IOException;

public interface SlimeLoader {

    public SlimeWorld loadWorld(String worldName, SlimeWorld.SlimeProperties properties) throws UnknownWorldException, IOException, CorruptedWorldException;
    public boolean worldExists(String worldName);
    public void saveWorld(SlimeWorld world) throws IOException;
}
