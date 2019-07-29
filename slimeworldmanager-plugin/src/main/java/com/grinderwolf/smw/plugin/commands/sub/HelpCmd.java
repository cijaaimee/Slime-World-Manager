package com.grinderwolf.smw.plugin.commands.sub;

import com.grinderwolf.smw.plugin.commands.CommandManager;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Getter
public class HelpCmd implements Subcommand {

    private final String usage = "help";
    private final String description = "Shows this page.";

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        sender.sendMessage(CommandManager.PREFIX + "Command list:");

        for (Subcommand cmd : CommandManager.getInstance().getCommands()) {
            if (cmd.inGameOnly() && !(sender instanceof Player) || (!cmd.getPermission().equals("") && !sender.hasPermission(cmd.getPermission()) && !sender.hasPermission("smw.*"))) {
                continue;
            }

            sender.sendMessage(ChatColor.GRAY + "  -" + ChatColor.AQUA + "/smw " + cmd.getUsage() + ChatColor.GRAY + " - " + cmd.getDescription());
        }

        return true;
    }
}
