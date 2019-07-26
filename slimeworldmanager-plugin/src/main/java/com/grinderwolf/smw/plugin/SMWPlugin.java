package com.grinderwolf.smw.plugin;

import com.grinderwolf.smw.api.SlimeWorld;
import com.grinderwolf.smw.api.exceptions.CorruptedWorldException;
import com.grinderwolf.smw.api.exceptions.InvalidVersionException;
import com.grinderwolf.smw.api.exceptions.UnknownWorldException;
import com.grinderwolf.smw.nms.SlimeNMS;
import com.grinderwolf.smw.nms.v1_8_R3.v1_8_R3SlimeNMS;
import com.grinderwolf.smw.plugin.loaders.Loaders;
import com.grinderwolf.smw.plugin.log.Logging;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class SMWPlugin extends JavaPlugin {

    @Getter
    private static SMWPlugin instance;
    @Getter
    private SlimeNMS nms;

    private SlimeWorld world;

    @Override
    public void onLoad() {
        Logging.info("Loading...");
        instance = this;

        try {
            nms = loadInjector();
        } catch (InvalidVersionException ex) {
            Logging.error("Couldn't load injector:");
            ex.printStackTrace();

            return;
        }

        nms.inject(); // Useless for now
    }

    @Override
    public void onEnable() {
        if (nms == null) {
            this.setEnabled(false);
            return;
        }

        Logging.info("Loading test world.");
        try {
            world = Loaders.FILE.getInstance().loadWorld("test");
            nms.loadWorld(world);
        } catch (UnknownWorldException e) {
            Logging.error("World not found.");
        } catch (IOException e) {
            Logging.error("Couldn't load world:");
            e.printStackTrace();
        } catch (CorruptedWorldException e) {
            Logging.error("World is corrupt!");
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {

    }

    private SlimeNMS loadInjector() throws InvalidVersionException {
        String version = Bukkit.getServer().getClass().getPackage().getName();
        String nmsVersion = version.substring(version.lastIndexOf('.') + 1);

        Logging.info("Minecraft version: " + nmsVersion);

        switch (nmsVersion) {
            case "v1_8_R3":
                return new v1_8_R3SlimeNMS();
            default:
                throw new InvalidVersionException(nmsVersion);
        }
    }
}
