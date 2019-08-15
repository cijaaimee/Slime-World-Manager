package com.grinderwolf.swm.plugin.commands.sub;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.grinderwolf.swm.api.exceptions.InvalidWorldException;
import com.grinderwolf.swm.api.exceptions.WorldAlreadyExistsException;
import com.grinderwolf.swm.api.exceptions.WorldLoadedException;
import com.grinderwolf.swm.api.exceptions.WorldTooBigException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.loaders.LoaderUtils;
import com.grinderwolf.swm.plugin.log.Logging;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Getter
public class ImportWorldCmd implements Subcommand {

    private final String usage = "import <path-to-world> <data-source> [new-world-name]";
    private final String description = "Convert a world to the slime format and save it.";
    private final String permission = "swm.importworld";

    private final Cache<String, String[]> importCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length > 1) {
            String loaderString = args[1];
            SlimeLoader loader = LoaderUtils.getLoader(loaderString);

            if (loader == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Data source " + loaderString + " does not exist.");

                return true;
            }

            File worldDir = new File(args[0]);

            if (!worldDir.exists() || !worldDir.isDirectory()) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Path " + worldDir.getPath() + " does not point out to a valid world directory.");

                return true;
            }

            String[] oldArgs = importCache.getIfPresent(sender.getName());

            if (oldArgs != null) {
                importCache.invalidate(sender.getName());

                if (Arrays.equals(args, oldArgs)) { // Make sure it's exactly the same command
                    String worldName = (args.length > 2 ? args[2] : worldDir.getName());

                    sender.sendMessage(Logging.COMMAND_PREFIX + "Importing world " + worldDir.getName() + " into data source " + loaderString + "...");

                    Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {

                        try {
                            long start = System.currentTimeMillis();
                            SWMPlugin.getInstance().importWorld(worldDir, worldName, loader);

                            sender.sendMessage(Logging.COMMAND_PREFIX +  ChatColor.GREEN + "World " + ChatColor.YELLOW + worldName + ChatColor.GREEN + " imported " +
                                    "successfully in " + (System.currentTimeMillis() - start) + "ms. Remember to add it to the worlds config file before loading it.");
                        } catch (WorldAlreadyExistsException e) {
                            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Data source " + loaderString + " already contains a world called " + worldName + ".");
                        } catch (InvalidWorldException e) {
                            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Directory " + worldDir.getName() + " does not contain a valid Minecraft world.");
                        } catch (WorldLoadedException ex) {
                            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldDir.getName() + " is loaded on this server. Please unload it before importing it.");
                        } catch (WorldTooBigException ex) {
                            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Hey! Didn't you just read the warning? The Slime Format isn't meant for big worlds." +
                                    " The world you provided just breaks everything. Please, trim it by using the MCEdit tool and try again.");
                        } catch (IOException ex) {
                            if (!(sender instanceof ConsoleCommandSender)) {
                                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to import world " + worldName
                                        + ". Take a look at the server console for more information.");
                            }

                            Logging.error("Failed to import world " + worldName + ". Stack trace:");
                            ex.printStackTrace();
                        }

                    });

                    return true;
                }
            }

            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + ChatColor.BOLD + "WARNING: " + ChatColor.GRAY + "The Slime Format is meant to " +
                    "be used on tiny maps, not big survival worlds. It is recommended to trim your world by using the Prune MCEdit tool to ensure " +
                    "you don't save more chunks than you want to.");

            sender.sendMessage(" ");
            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.YELLOW + ChatColor.BOLD + "NOTE: " + ChatColor.GRAY + "This command will automatically ignore every " +
                    "chunk that doesn't contain any blocks.");
            sender.sendMessage(" ");
            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GRAY + "If you are sure you want to continue, type again this command.");

            importCache.put(sender.getName(), args);

            return true;
        }

        return false;
    }
}

