package com.grinderwolf.smw.plugin.loaders;

import com.grinderwolf.smw.api.exceptions.UnknownWorldException;
import com.grinderwolf.smw.api.loaders.SlimeLoader;
import com.grinderwolf.smw.plugin.log.Logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

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
    public byte[] loadWorld(String worldName) throws UnknownWorldException, IOException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        File file = new File(WORLD_DIR, worldName + ".slime");

        return Files.readAllBytes(file.toPath());
    }

    @Override
    public boolean worldExists(String worldName) {
        return new File(WORLD_DIR, worldName + ".slime").exists();
    }

    @Override
    public void saveWorld(String worldName, byte[] serializedWorld) throws IOException {
        File lastBackup = new File(WORLD_DIR, worldName + ".slime_old");

        if (lastBackup.exists()) {
            lastBackup.delete();
        }

        File file = new File(WORLD_DIR, worldName + ".slime");

        file.renameTo(lastBackup);

        try (FileOutputStream fileStream = new FileOutputStream(file, false)) {
            fileStream.write(serializedWorld);
            fileStream.flush();
            lastBackup.delete();
        }
    }
}
