package org.josefadventures.townychunkloader.Command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.josefadventures.townychunkloader.TownyChunkLoader;

public class CommandHelp extends SubCommand {

    private static final String PERMISSION = "townychunkloader.command";

    public CommandHelp(TownyChunkLoader plugin) {
        super(plugin);
    }

    @Override
    public boolean hasAccess(CommandSender sender) {
        if (super.hasAccess(sender)) {
            return sender.hasPermission(PERMISSION);
        } else {
            return false;
        }
    }

    @Override
    public boolean handleAccess(CommandSender sender) {
        if (sender.hasPermission(PERMISSION)) {
            return true;
        } else {
            this.sendPermissionMessage(sender);
            return false;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {

        sender.sendMessage(ChatColor.DARK_GREEN + "TownyChunkLoader commands:");
        sender.sendMessage(ChatColor.GREEN + "/cl help " + ChatColor.DARK_GREEN + "- Display this message.");
        sender.sendMessage(ChatColor.GREEN + "/cl list " + ChatColor.DARK_GREEN + "- List your own chunk loaders.");

        if (sender.hasPermission(PERMISSION_COMMAND_LIST_ALL))
            sender.sendMessage(ChatColor.GREEN + "/cl list all " + ChatColor.DARK_GREEN + "- List all chunk loaders on the server.");
        if (sender.hasPermission(PERMISSION_COMMAND_LIST_PLAYER))
            sender.sendMessage(ChatColor.GREEN + "/cl list player <player> " + ChatColor.DARK_GREEN + "- List all chunk loaders owned by specified player.");

        sender.sendMessage(ChatColor.GREEN + "/cl set " + ChatColor.DARK_GREEN + "- Attempt to set current chunk as chunk loader.");
        sender.sendMessage(ChatColor.GREEN + "/cl delete [chunk name] " + ChatColor.DARK_GREEN + "- Remove chunk loader from current chunk, or from chunk with name.");
        sender.sendMessage(ChatColor.GREEN + "/cl bump " + ChatColor.DARK_GREEN + "- Resets timers for all chunk loaders.");

        if (sender.hasPermission(PERMISSION_COMMAND_HERE))
            sender.sendMessage(ChatColor.GREEN + "/cl here " + ChatColor.DARK_GREEN + "- Check if there's a chunk loader in the current chunk and gives info about the chunk loader.");
        else
            sender.sendMessage(ChatColor.GREEN + "/cl here " + ChatColor.DARK_GREEN + "- Check if there's a chunk loader in the current chunk.");

        if (sender.hasPermission(PERMISSION_COMMAND_FORCEDELETE)) {
            sender.sendMessage(ChatColor.GREEN + "/cl forcedelete " + ChatColor.DARK_GREEN + "- Remove all chunk loaders on the server.");
            sender.sendMessage(ChatColor.GREEN + "/cl forcedelete world <world> " + ChatColor.DARK_GREEN + "- Remove all chunk loaders in the specified world.");
            sender.sendMessage(ChatColor.GREEN + "/cl forcedelete player <player> " + ChatColor.DARK_GREEN + "- Remove all chunk loaders owned by specified player.");
        }


        return false;
    }

}
