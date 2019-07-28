package com.grinderwolf.smw.plugin.commands.sub;


import com.grinderwolf.smw.api.exceptions.CorruptedWorldException;
import com.grinderwolf.smw.api.exceptions.NewerFormatException;
import com.grinderwolf.smw.api.exceptions.UnknownWorldException;
import com.grinderwolf.smw.api.exceptions.WorldInUseException;
import com.grinderwolf.smw.plugin.SMWPlugin;
import com.grinderwolf.smw.plugin.commands.CommandManager;
import com.grinderwolf.smw.plugin.config.ConfigManager;
import com.grinderwolf.smw.plugin.log.Logging;
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
    private final String permission = "smw.loadworld";

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length > 0) {
            String worldName = args[0];
            World world = Bukkit.getWorld(worldName);

            if (world != null) {
                sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + "World " + worldName + " is already loaded!");

                return true;
            }

            ConfigurationSection worldConfig;

            try {
                ConfigurationSection config = ConfigManager.getFile("worlds").getConfigurationSection("worlds");

                if (config == null) {
                    sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + "The main config section seems to be missing. Make sure everything is where it's supposed to be.");

                    return true;
                }

                worldConfig = config.getConfigurationSection(worldName);
            } catch (IOException ex) {
                sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + "Failed to load the worlds config file. Take a look at the server console for more information.");
                Logging.error("Failed to load the worlds config file:");
                ex.printStackTrace();

                return true;
            }

            if (worldConfig == null) {
                sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + "Unknown world " + worldName + "! Are you sure you configured it correctly?");

                return true;
            }

            sender.sendMessage(CommandManager.PREFIX + ChatColor.GRAY + "Loading world " + worldName + "...");

            try {
                long start = System.currentTimeMillis();
                // This retrieves the world data through the specified loader on the main thread. Maybe in the future this should be performed asynchronously?
                SMWPlugin.getInstance().loadWorldFromConfig(worldConfig);
                sender.sendMessage(CommandManager.PREFIX + ChatColor.GRAY + "World " + worldName + " loaded in " + (System.currentTimeMillis() - start) + "ms!");
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + "Failed to load world " + worldName + ": " + ex.getMessage() + ".");
            } catch (CorruptedWorldException ex) {
                if (!(sender instanceof ConsoleCommandSender)) {
                    sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + "Failed to load world " + worldName +
                            ": world seems to be corrupted.");
                }

                Logging.error("Failed to load world " + worldName + ": world seems to be corrupted.");
                ex.printStackTrace();
            } catch (NewerFormatException ex) {
                sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + "Failed to load world " + worldName + ": this world" +
                        " was serialized with a newer version of the Slime Format (" + ex.getMessage() + ") that SMW cannot understand.");
            }catch (UnknownWorldException e) {
                sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + "Failed to load world " + worldName +
                        ": world could not be found (using loader '" + worldConfig.getString("loader") + "').");
            } catch (WorldInUseException e) {
                sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + "Failed to load world " + worldName +
                        ": world is already in use. If you are sure this is a mistake, run the command /smw manualunlock " + worldName + " " + worldConfig.get("loader"));
            } catch (IOException ex) {
                if (!(sender instanceof ConsoleCommandSender)) {
                    sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + "Failed to load world " + worldName
                            + ". Take a look at the server console for more information.");
                }

                Logging.error("Failed to load world " + worldName + ":");
                ex.printStackTrace();
            }

            return true;
        }

        return false;
    }
}

