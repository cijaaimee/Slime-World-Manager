package com.grinderwolf.smw.api;

import com.grinderwolf.smw.api.exceptions.CorruptedWorldException;
import com.grinderwolf.smw.api.exceptions.NewerFormatException;
import com.grinderwolf.smw.api.exceptions.UnknownWorldException;
import com.grinderwolf.smw.api.loaders.SlimeLoader;
import com.grinderwolf.smw.api.world.SlimeWorld;

import java.io.IOException;

public interface SlimePlugin {

    public SlimeWorld loadWorld(SlimeLoader loader, String worldName, SlimeWorld.SlimeProperties properties) throws UnknownWorldException, IOException, CorruptedWorldException, NewerFormatException;
}
