package com.grinderwolf.swm.plugin.world;

import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.log.Logging;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;

import java.io.IOException;

public class WorldUnlocker implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        SlimeWorld world = SWMPlugin.getInstance().getNms().getSlimeWorld(event.getWorld());

        if (world != null) {
            Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {

                try {
                    world.getLoader().unlockWorld(world.getName());
                } catch (IOException ex) {
                    Logging.error("Failed to unlock world " + world.getName() + ". Please unlock it manually by using the command /swm unlock. Stack trace:");
                    ex.printStackTrace();
                } catch (UnknownWorldException ignored) {

                }

            });
        }
    }
}
