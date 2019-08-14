package com.grinderwolf.swm.plugin.commands.sub;

import com.grinderwolf.swm.plugin.log.Logging;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
}
