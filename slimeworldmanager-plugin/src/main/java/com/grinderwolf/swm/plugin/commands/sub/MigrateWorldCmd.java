package com.grinderwolf.swm.plugin.commands.sub;

import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.WorldAlreadyExistsException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.commands.CommandManager;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.loaders.LoaderUtils;
import com.grinderwolf.swm.plugin.log.Logging;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;

@Getter
public class MigrateWorldCmd implements Subcommand {

    private final String usage = "migrate <world> <new-data-source>";
    private final String description = "Migrate a world from one data source to another.";
    private final String permission = "swm.migrate";

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length > 1) {
            FileConfiguration configFile;

            try {
                configFile = ConfigManager.getFile("worlds");
            } catch (IOException ex) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load the worlds config file. Take a look at the server console for more information.");
                Logging.error("Failed to load the worlds config file:");
                ex.printStackTrace();

                return true;
            }

            ConfigurationSection config = configFile.getConfigurationSection("worlds");

            if (config == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "The main config section seems to be missing. Make sure everything is where it's supposed to be.");

                return true;
            }

            String worldName = args[0];
            ConfigurationSection worldConfig = config.getConfigurationSection(worldName);

            if (worldConfig == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown world " + worldName + "! Are you sure you configured it correctly?");

                return true;
            }

            String loaderString = args[1];
            SlimeLoader newLoader = LoaderUtils.getLoader(loaderString);

            if (newLoader == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown data source " + loaderString + "!");

                return true;
            }

            if (loaderString.equalsIgnoreCase(worldConfig.getString("source"))) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is already stored using data source " + loaderString + "!");

                return true;
            }

            String oldLoaderString = worldConfig.getString("source");
            SlimeLoader oldLoader = LoaderUtils.getLoader(oldLoaderString);

            if (oldLoader == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown data source " + loaderString + "! Are you sure you configured it correctly?");

                return true;
            }

            if (CommandManager.getInstance().getWorldsInUse().contains(worldName)) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is already being used on another command! Wait some time and try again.");

                return true;
            }

            CommandManager.getInstance().getWorldsInUse().add(worldName);

            Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {

                try {
                    long start = System.currentTimeMillis();
                    SWMPlugin.getInstance().migrateWorld(worldName, oldLoader, newLoader);

                    worldConfig.set("source", loaderString);
                    ConfigManager.saveFile(configFile, "worlds");

                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GREEN + "World " + ChatColor.YELLOW + worldName + ChatColor.GREEN + " migrated in "
                            + (System.currentTimeMillis() - start) + "ms!");
                } catch (IOException ex) {
                    if (!(sender instanceof ConsoleCommandSender)) {
                        sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to migrate world " + worldName + " (using data sources "
                                + oldLoaderString + " and " + loaderString + "). Take a look at the server console for more information.");
                    }

                    Logging.error("Failed to load world " + worldName + " (using data source " + oldLoaderString + "):");
                    ex.printStackTrace();
                } catch (WorldInUseException ex) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is being used on another server.");
                } catch (WorldAlreadyExistsException ex) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Data source " + loaderString + " already contains a world named " + worldName + "!");
                } catch (UnknownWorldException ex) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Can't find world " + worldName + " in data source " + oldLoaderString + ".");
                } finally {
                    CommandManager.getInstance().getWorldsInUse().remove(worldName);
                }

            });

            return true;
        }

        return false;
    }
}

