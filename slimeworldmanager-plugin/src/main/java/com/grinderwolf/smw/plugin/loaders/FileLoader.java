package com.grinderwolf.smw.plugin.loaders;

import com.grinderwolf.smw.api.SlimeLoader;
import com.grinderwolf.smw.api.SlimeWorld;
import com.grinderwolf.smw.api.exceptions.CorruptedWorldException;
import com.grinderwolf.smw.api.exceptions.UnknownWorldException;
import com.grinderwolf.smw.nms.CraftSlimeWorld;
import com.grinderwolf.smw.plugin.log.Logging;

import java.io.*;

public class FileLoader implements SlimeLoader {

    private static final File WORLD_DIR = new File("slime_worlds");

    {
        if (WORLD_DIR.exists() && !WORLD_DIR.isDirectory()) {
            Logging.warning("A file named '" + WORLD_DIR.getName() + "' has been deleted, as this is the name used for the worlds directory.");
            WORLD_DIR.delete();
        }

        WORLD_DIR.mkdirs();
    }

    @Override
    public SlimeWorld loadWorld(String worldName) throws UnknownWorldException, IOException, CorruptedWorldException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        File file = new File(WORLD_DIR, worldName + ".slime");

        try (FileInputStream fileStream = new FileInputStream(file);
             DataInputStream dataStream = new DataInputStream(fileStream)) {
            return LoaderUtils.loadWorldFromStream(this, worldName, dataStream);
        }
    }

    @Override
    public boolean worldExists(String worldName) {
        return new File(WORLD_DIR, worldName + ".slime").exists();
    }

    @Override
    public void saveWorld(SlimeWorld world) throws IOException {
        File file = new File(WORLD_DIR, world.getName() + ".slime");

        try (FileOutputStream fileStream = new FileOutputStream(file, false)) {
            fileStream.write(LoaderUtils.serializeWorld((CraftSlimeWorld) world));
            fileStream.flush();
        }
    }
}
