package com.grinderwolf.swm.plugin.commands.sub;


import com.grinderwolf.swm.api.exceptions.CorruptedWorldException;
import com.grinderwolf.swm.api.exceptions.NewerFormatException;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.UnsupportedWorldException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.commands.CommandManager;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.log.Logging;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.io.IOException;

@Getter
public class LoadWorldCmd implements Subcommand {

    private final String usage = "load <world>";
    private final String description = "Load a world.";
    private final String permission = "swm.loadworld";

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length > 0) {
            String worldName = args[0];
            World world = Bukkit.getWorld(worldName);

            if (world != null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is already loaded!");

                return true;
            }

            ConfigurationSection worldConfig;

            try {
                ConfigurationSection config = ConfigManager.getFile("worlds").getConfigurationSection("worlds");

                if (config == null) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "The main config section seems to be missing. Make sure everything is where it's supposed to be.");

                    return true;
                }

                worldConfig = config.getConfigurationSection(worldName);
            } catch (IOException ex) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load the worlds config file. Take a look at the server console for more information.");
                Logging.error("Failed to load the worlds config file:");
                ex.printStackTrace();

                return true;
            }

            if (worldConfig == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown world " + worldName + "! Are you sure you configured it correctly?");

                return true;
            }

            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GRAY + "Loading world " + ChatColor.YELLOW + worldName + ChatColor.GRAY + "...");

            // It's best to load the world async, and then just go back to the server thread and add it to the world list
            Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {
                try {
                    long start = System.currentTimeMillis();
                    SlimeWorld slimeWorld = SWMPlugin.getInstance().loadWorldFromConfig(worldConfig);

                    Bukkit.getScheduler().runTask(SWMPlugin.getInstance(), () -> {
                        SWMPlugin.getInstance().generateWorld(slimeWorld);

                        sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GREEN + "World " + ChatColor.YELLOW + worldName
                                + ChatColor.GREEN + " loaded and generated in " + (System.currentTimeMillis() - start) + "ms!");
                    });
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName + ": " + ex.getMessage() + ".");
                } catch (CorruptedWorldException ex) {
                    if (!(sender instanceof ConsoleCommandSender)) {
                        sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName +
                                ": world seems to be corrupted.");
                    }

                    Logging.error("Failed to load world " + worldName + ": world seems to be corrupted.");
                    ex.printStackTrace();
                } catch (NewerFormatException ex) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName + ": this world" +
                            " was serialized with a newer version of the Slime Format (" + ex.getMessage() + ") that SWM cannot understand.");
                } catch (UnknownWorldException e) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName +
                            ": world could not be found (using data source '" + worldConfig.getString("source") + "').");
                } catch (WorldInUseException e) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName +
                            ": world is already in use. If you are sure this is a mistake, run the command /swm unlock " + worldName + " " + worldConfig.get("source"));
                } catch (UnsupportedWorldException e) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName + ": world is meant to be used on a "
                            + (e.isV1_13() ? "1.13 or newer" : "1.12.2 or older") + " server.");
                } catch (IOException ex) {
                    if (!(sender instanceof ConsoleCommandSender)) {
                        sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName
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
}

