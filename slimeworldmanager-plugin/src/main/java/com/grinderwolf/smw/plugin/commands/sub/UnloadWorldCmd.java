package com.grinderwolf.smw.plugin.commands.sub;


import com.grinderwolf.smw.plugin.commands.CommandManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

@Getter
public class UnloadWorldCmd implements Subcommand {

    private final String usage = "unload <world>";
    private final String description = "Unload a world.";
    private final String permission = "smw.unloadworld";

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length > 0) {
            World world = Bukkit.getWorld(args[0]);

            if (world == null) {
                sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + "World " + args[0] + " is not loaded!");

                return true;
            }

            Bukkit.unloadWorld(world, true);
            sender.sendMessage(CommandManager.PREFIX + ChatColor.RED + "World " + args[0] + " unloaded correctly.");

            return true;
        }

        return false;
    }
}

