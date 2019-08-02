package com.grinderwolf.swm.plugin.commands.sub;

import org.bukkit.command.CommandSender;

public interface Subcommand {

    public boolean onCommand(CommandSender sender, String[] args);

    public String getUsage();
    public String getDescription();

    default boolean inGameOnly() {
        return false;
    }

    default String getPermission() {
        return "";
    }
}
