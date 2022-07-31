/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.plugin.commands.sub;

import com.grinderwolf.swm.api.exceptions.WorldAlreadyExistsException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.commands.CommandManager;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.config.WorldData;
import com.grinderwolf.swm.plugin.config.WorldsConfig;
import com.grinderwolf.swm.plugin.log.Logging;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Getter
public class CreateWorldCmd implements Subcommand {

    private final String usage = "create <world> <data-source>";
    private final String description = "Create an empty world.";
    private final String permission = "swm.createworld";

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length > 1) {
            String worldName = args[0];

            if (CommandManager.getInstance().getWorldsInUse().contains(worldName)) {
                sender.sendMessage(Logging.COMMAND_PREFIX
                        + ChatColor.RED
                        + "World "
                        + worldName
                        + " is already being used on another command! Wait some time and try again.");

                return true;
            }

            World world = Bukkit.getWorld(worldName);

            if (world != null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " already exists!");

                return true;
            }

            WorldsConfig config = ConfigManager.getWorldConfig();

            if (config.getWorlds().containsKey(worldName)) {
                sender.sendMessage(Logging.COMMAND_PREFIX
                        + ChatColor.RED
                        + "There is already a world called  "
                        + worldName
                        + " inside the worlds config file.");

                return true;
            }

            String dataSource = args[1];
            SlimeLoader loader = SWMPlugin.getInstance().getLoader(dataSource);

            if (loader == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown data source  " + dataSource + ".");

                return true;
            }

            CommandManager.getInstance().getWorldsInUse().add(worldName);
            sender.sendMessage(Logging.COMMAND_PREFIX
                    + ChatColor.GRAY
                    + "Creating empty world "
                    + ChatColor.YELLOW
                    + worldName
                    + ChatColor.GRAY
                    + "...");

            // It's best to load the world async, and then just go back to the server thread and add
            // it to the world list
            Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {
                try {
                    long start = System.currentTimeMillis();

                    WorldData worldData = new WorldData();
                    worldData.setSpawn("0, 64, 0");

                    SlimePropertyMap propertyMap = worldData.toPropertyMap();
                    SlimeWorld slimeWorld =
                            SWMPlugin.getInstance().createEmptyWorld(loader, worldName, false, propertyMap);

                    Bukkit.getScheduler().runTask(SWMPlugin.getInstance(), () -> {
                        try {
                            SWMPlugin.getInstance().generateWorld(slimeWorld);

                            // Bedrock block
                            Location location = new Location(Bukkit.getWorld(worldName), 0, 61, 0);
                            location.getBlock().setType(Material.BEDROCK);

                            // Config
                            config.getWorlds().put(worldName, worldData);
                            config.save();

                            sender.sendMessage(Logging.COMMAND_PREFIX
                                    + ChatColor.GREEN
                                    + "World "
                                    + ChatColor.YELLOW
                                    + worldName
                                    + ChatColor.GREEN
                                    + " created in "
                                    + (System.currentTimeMillis() - start)
                                    + "ms!");
                        } catch (IllegalArgumentException ex) {
                            sender.sendMessage(Logging.COMMAND_PREFIX
                                    + ChatColor.RED
                                    + "Failed to create world "
                                    + worldName
                                    + ": "
                                    + ex.getMessage()
                                    + ".");
                        }
                    });
                } catch (WorldAlreadyExistsException ex) {
                    sender.sendMessage(Logging.COMMAND_PREFIX
                            + ChatColor.RED
                            + "Failed to create world "
                            + worldName
                            + ": world already exists (using data source '"
                            + dataSource
                            + "').");
                } catch (IOException ex) {
                    if (!(sender instanceof ConsoleCommandSender)) {
                        sender.sendMessage(Logging.COMMAND_PREFIX
                                + ChatColor.RED
                                + "Failed to create world "
                                + worldName
                                + ". Take a look at the server console for more information.");
                    }

                    Logging.error("Failed to load world " + worldName + ":");
                    ex.printStackTrace();
                } finally {
                    CommandManager.getInstance().getWorldsInUse().remove(worldName);
                }
            });

            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
