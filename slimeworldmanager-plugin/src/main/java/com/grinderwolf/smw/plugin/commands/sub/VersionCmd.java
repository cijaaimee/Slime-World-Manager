package com.grinderwolf.smw.plugin.commands.sub;

import com.grinderwolf.smw.api.utils.SlimeFormat;
import com.grinderwolf.smw.plugin.SMWPlugin;
import com.grinderwolf.smw.plugin.commands.CommandManager;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

@Getter
public class VersionCmd implements Subcommand {

    private final String usage = "version";
    private final String description = "Shows the plugin version.";

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        sender.sendMessage(CommandManager.PREFIX + ChatColor.GRAY + "This server is running SMW " + ChatColor.YELLOW + "v" + SMWPlugin.getInstance()
                .getDescription().getVersion() + ChatColor.GRAY + ", which supports up to Slime Format " + ChatColor.AQUA + "v" + SlimeFormat.SLIME_VERSION + ChatColor.GRAY + ".");

        return true;
    }
}
