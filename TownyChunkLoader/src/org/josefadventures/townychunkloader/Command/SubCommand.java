package org.josefadventures.townychunkloader.Command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.josefadventures.townychunkloader.TownyChunkLoader;

public abstract class SubCommand {

    private static final String MESSAGE_PERMISSION = ChatColor.DARK_RED + "You do not have access to use this command.";
    private static final String MESSAGE_USAGE = ChatColor.RED + "Wrong usage of the command. Use /cl help to list all commands.";


    private TownyChunkLoader plugin;
    private boolean allowConsole = true;

    SubCommand(TownyChunkLoader plugin) {
        this.plugin = plugin;
    }

    SubCommand(TownyChunkLoader plugin, boolean allowConsole) {
        this(plugin);
        this.allowConsole = allowConsole;
    }

    /**
     * Checks if sender has access to use the sub-command.
     * @return Whether sender can use the sub-command.
     */
    public boolean hasAccess(CommandSender sender) {
        if (this.allowConsole) {
            return true;
        } else {
            return (sender instanceof Player);
        }
    }

    /**
     * Checks if sender has access to use the command. Has
     * permission or is console, etc. Should send messages
     * to the sender.
     * @return Whether sender can use the command.
     */
    public abstract boolean handleAccess(CommandSender sender);

    /**
     * Handles the command execution.
     * @param sender Who executed the command.
     * @param args Sub-command arguments.
     * @return Success or not. Should always be true.
     */
    public abstract boolean onCommand(CommandSender sender, String[] args);

    public void sendPermissionMessage(CommandSender sender) {
        sender.sendMessage(MESSAGE_PERMISSION);
    }

    public void sendUsageMessage(CommandSender sender) {
        sender.sendMessage(MESSAGE_USAGE);
    }

}
