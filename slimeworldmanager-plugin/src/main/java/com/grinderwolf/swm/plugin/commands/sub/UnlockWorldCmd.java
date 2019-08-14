package com.grinderwolf.swm.plugin.commands.sub;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.commands.CommandManager;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.loaders.LoaderUtils;
import com.grinderwolf.swm.plugin.log.Logging;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Getter
public class UnlockWorldCmd implements Subcommand {

    private final String usage = "unlock <world> [data-source]";
    private final String description = "Unlock a world manually.";
    private final String permission = "swm.unlockworld";

    private final Cache<String, String[]> unlockCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length > 0) {
            String worldName = args[0];
            World world = Bukkit.getWorld(worldName);

            if (world != null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is loaded on this server!");

                return true;
            }

            if (CommandManager.getInstance().getWorldsInUse().contains(worldName)) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is already being used on another command! Wait some time and try again.");

                return true;
            }

            String loaderString;

            if (args.length > 1) {
                loaderString = args[1];
            } else {
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
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown world " + worldName + "! Are you sure you've typed it correctly?");

                    return true;
                }

                loaderString = worldConfig.getString("source");
            }

            SlimeLoader loader = LoaderUtils.getLoader(loaderString);

            if (loader == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Data source " + loaderString + " does not exist.");

                return true;
            }

            String[] oldArgs = unlockCache.getIfPresent(sender.getName());

            if (oldArgs != null) {
                unlockCache.invalidate(sender.getName());

                if (Arrays.equals(args, oldArgs)) { // Make sure it's exactly the same command
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GRAY + "Unlocking world " + ChatColor.YELLOW + worldName + ChatColor.GRAY + "...");

                    // No need to do this synchronously
                    CommandManager.getInstance().getWorldsInUse().add(worldName);
                    Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {

                        try {
                            if (loader.isWorldLocked(worldName)) {
                                loader.unlockWorld(worldName);
                                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GREEN + "World " + ChatColor.YELLOW + worldName + ChatColor.GREEN + " unlocked.");
                            } else {
                                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + loaderString + " is not locked!");
                            }
                        } catch (IOException ex) {
                            if (!(sender instanceof ConsoleCommandSender)) {
                                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to unlock world " + worldName
                                        + ". Take a look at the server console for more information.");
                            }

                            Logging.error("Failed to unlock world " + worldName + ". Stack trace:");
                            ex.printStackTrace();
                        } catch (UnknownWorldException ex) {
                            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Data source " + loaderString + " does not contain any world called " + worldName + ".");
                        } finally {
                            CommandManager.getInstance().getWorldsInUse().remove(worldName);
                        }

                    });

                    return true;
                }
            }

            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + ChatColor.BOLD + "WARNING: " + ChatColor.GRAY + "Unlocking worlds manually " +
                    "should only be done in case automatic unlocking has failed. Please " + ChatColor.GREEN + ChatColor.BOLD + ChatColor.UNDERLINE
                    + "make sure" + ChatColor.GRAY + " no server is using this world in write mode.");

            sender.sendMessage(" ");
            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GRAY + "If you are sure you want to continue, type again this command.");

            unlockCache.put(sender.getName(), args);

            return true;
        }

        return false;
    }
}

