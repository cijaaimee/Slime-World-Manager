package com.grinderwolf.smw.plugin.commands.sub;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.grinderwolf.smw.api.exceptions.UnknownWorldException;
import com.grinderwolf.smw.api.loaders.SlimeLoader;
import com.grinderwolf.smw.api.loaders.SlimeLoaders;
import com.grinderwolf.smw.plugin.SMWPlugin;
import com.grinderwolf.smw.plugin.commands.CommandManager;
import com.grinderwolf.smw.plugin.log.Logging;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Getter
public class UnlockWorldCmd implements Subcommand {

    private final String usage = "manualunlock <world> <loader>";
    private final String description = "Unlock a world manually.";
    private final String permission = "smw.unlockworld";

    private final Cache<String, String[]> unlockCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();
    private final List<String> unlockingWorlds = new ArrayList<>();

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length != 2) {
            String loaderString = args[1];
            SlimeLoader loader = SlimeLoaders.get(loaderString);

            if (loader == null) {
                sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + "Data source " + loaderString + " does not exist.");

                return true;
            }

            String worldName = args[0];

            World world = Bukkit.getWorld(worldName);

            if (world != null) {
                sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + "World " + worldName + " is loaded on this server!");

                return true;
            }

            if (unlockingWorlds.contains(worldName)) {
                sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + "World " + worldName + " is already being unlocked!");

                return true;
            }

            String[] oldArgs = unlockCache.getIfPresent(sender.getName());

            if (oldArgs != null) {
                unlockCache.invalidate(sender.getName());

                if (Arrays.equals(args, oldArgs)) { // Make sure it's exactly the same command
                    sender.sendMessage(CommandManager.PREFIX + ChatColor.GRAY + "Unlocking world " + worldName + "...");

                    // No need to do this synchronously
                    unlockingWorlds.add(worldName);
                    Bukkit.getScheduler().runTaskAsynchronously(SMWPlugin.getInstance(), () -> {

                        try {
                            if (loader.isWorldLocked(worldName)) {
                                loader.unlockWorld(worldName);
                                sender.sendMessage(CommandManager.PREFIX + ChatColor.GRAY + "World " + worldName + " unlocked.");
                            } else {
                                sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + "World " + loaderString + " is not locked!");
                            }
                        } catch (IOException ex) {
                            if (!(sender instanceof ConsoleCommandSender)) {
                                sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + "Failed to unlock world " + worldName
                                        + ". Take a look at the server console for more information.");
                            }

                            Logging.error("Failed to unlock world " + worldName + ". Stack trace:");
                            ex.printStackTrace();
                        } catch (UnknownWorldException ex) {
                            sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + "Data source " + loaderString + " does not contain any world called " + worldName + ".");
                        } finally {
                            unlockingWorlds.remove(worldName);
                        }

                    });

                    return true;
                }
            }

            sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + ChatColor.BOLD + "WARNING: " + ChatColor.GRAY + "Unlocking worlds manually " +
                    "should only be done in case automatic unlocking has failed. Please " + ChatColor.GREEN + ChatColor.BOLD + ChatColor.UNDERLINE
                    + "make sure" + ChatColor.GRAY + " no server is using this world in write mode.");

            sender.sendMessage(" ");
            sender.sendMessage(CommandManager.PREFIX + ChatColor.GRAY + "If you are sure you want to continue, type again this command.");

            unlockCache.put(sender.getName(), args);


            return true;
        }

        return false;
    }
}

