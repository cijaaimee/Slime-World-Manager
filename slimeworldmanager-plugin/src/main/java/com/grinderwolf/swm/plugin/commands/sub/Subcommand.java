package com.grinderwolf.swm.plugin.commands.sub;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface Subcommand {

    public boolean onCommand(CommandSender sender, String[] args);

    public List<String> onTabComplete(CommandSender sender, String[] args);

    public String getUsage();

    public String getDescription();

    default boolean inGameOnly() {
        return false;
    }

    default String getPermission() {
        return "";
    }
}
