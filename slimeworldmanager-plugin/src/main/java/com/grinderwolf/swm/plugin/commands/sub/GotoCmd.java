package com.grinderwolf.swm.plugin.commands.sub;

import com.grinderwolf.swm.plugin.log.Logging;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Getter
public class GotoCmd implements Subcommand {

    private final String usage = "goto <world> [player]";
    private final String description = "Teleport yourself (or someone else) to a world.";
    private final String permission = "swm.goto";

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length > 0) {
            World world = Bukkit.getWorld(args[0]);

            if (world == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + args[0] + " does not exist!");

                return true;
            }

            Player target;

            if (args.length > 1) {
                target = Bukkit.getPlayerExact(args[1]);
            } else {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "The console cannot be teleported to a world! Please specify a player.");

                    return true;
                }

                target = (Player) sender;
            }

            if (target == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + args[1] + " is offline.");

                return true;
            }

            sender.sendMessage(Logging.COMMAND_PREFIX + "Teleporting " + (target.getName().equals(sender.getName())
                    ? "yourself" : ChatColor.YELLOW + target.getName() + ChatColor.GRAY) + " to " + ChatColor.AQUA + world.getName() + ChatColor.GRAY + "...");

            Location spawnLocation = world.getSpawnLocation();

            // Safe Spawn Location
            while (spawnLocation.getBlock().getType() != Material.AIR || spawnLocation.getBlock().getRelative(BlockFace.UP).getType() != Material.AIR) {
                spawnLocation.add(0, 1, 0);
            }

            target.teleport(spawnLocation);

            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> toReturn = null;

        if (sender instanceof ConsoleCommandSender) {
            return Collections.emptyList();
        }

        if (args.length == 2) {
            final String typed = args[1].toLowerCase();

            for (World world : Bukkit.getWorlds()) {
                final String worldName = world.getName();
                if (worldName.toLowerCase().startsWith(typed)) {
                    if (toReturn == null) {
                        toReturn = new LinkedList<>();
                    }
                    toReturn.add(worldName);
                }
            }
        }

        if (args.length == 3) {
            final String typed = args[2].toLowerCase();

            for (Player player : Bukkit.getOnlinePlayers()) {
                final String playerName = player.getName();
                if (playerName.toLowerCase().startsWith(typed)) {
                    if (toReturn == null) {
                        toReturn = new LinkedList<>();
                    }
                    toReturn.add(playerName);
                }
            }
        }

        return toReturn == null ? Collections.emptyList() : toReturn;
    }
}
