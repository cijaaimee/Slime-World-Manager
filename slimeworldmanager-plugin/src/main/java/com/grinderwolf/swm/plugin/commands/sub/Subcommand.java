/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

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
