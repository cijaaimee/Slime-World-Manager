package com.grinderwolf.smw.plugin.commands;

import com.grinderwolf.smw.plugin.SMWPlugin;
import com.grinderwolf.smw.plugin.commands.sub.GotoCmd;
import com.grinderwolf.smw.plugin.commands.sub.HelpCmd;
import com.grinderwolf.smw.plugin.commands.sub.LoadWorldCmd;
import com.grinderwolf.smw.plugin.commands.sub.Subcommand;
import com.grinderwolf.smw.plugin.commands.sub.UnloadWorldCmd;
import com.grinderwolf.smw.plugin.commands.sub.UnlockWorldCmd;
import com.grinderwolf.smw.plugin.commands.sub.VersionCmd;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CommandManager implements CommandExecutor {

    public static final String PREFIX = ChatColor.YELLOW + "[SMW] ";

    @Getter
    private static CommandManager instance;
    private Map<String, Subcommand> commands = new HashMap<>();

    public CommandManager() {
        instance = this;

        commands.put("help", new HelpCmd());
        commands.put("version", new VersionCmd());
        commands.put("goto", new GotoCmd());
        commands.put("load", new LoadWorldCmd());
        commands.put("unload", new UnloadWorldCmd());
        commands.put("unlock", new UnlockWorldCmd());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(PREFIX + ChatColor.GRAY + SMWPlugin.getInstance().getDescription().getDescription() + " To check out the help page, type /smw help.");

            return true;
        }

        Subcommand command = commands.get(args[0]);

        if (command == null) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Unknown command. To check out the help page, type /smw help.");

            return true;
        }

        if (command.inGameOnly() && !(sender instanceof Player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "This command can only be run in-game.");

            return true;
        }

        if (!command.getPermission().equals("") && !sender.hasPermission(command.getPermission()) && !sender.hasPermission("smw.*")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "You do not have permission to perform this command.");

            return true;
        }

        String[] subCmdArgs = new String[args.length - 1];
        System.arraycopy(args,1, subCmdArgs, 0, subCmdArgs.length);

        if (!command.onCommand(sender, subCmdArgs)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Command usage: /smw " + ChatColor.GRAY + command.getUsage() + ChatColor.RED + ".");
        }

        return true;
    }

    public Collection<Subcommand> getCommands() {
        return commands.values();
    }
}
