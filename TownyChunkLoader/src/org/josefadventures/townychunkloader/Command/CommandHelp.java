package org.josefadventures.townychunkloader.command;

import org.bukkit.command.CommandSender;
import org.josefadventures.townychunkloader.TownyChunkLoader;

public class CommandHelp {

    public static boolean onCommand(CommandSender sender) {
        sender.sendMessage("§aTownyChunkLoader commands:");
        sender.sendMessage("§2/cl help §a- Display this message.");
        sender.sendMessage("§2/cl list §a- List your own chunk loaders.");

        if (sender.hasPermission(TownyChunkLoader.PERMISSION_COMMAND_LIST_ALL))
            sender.sendMessage("§2/cl list all §a- List all chunk loaders on the server.");
        if (sender.hasPermission(TownyChunkLoader.PERMISSION_COMMAND_LIST_PLAYER))
            sender.sendMessage("§2/cl list player <player> §a- List all chunk loaders owned by specified player.");

        sender.sendMessage("§2/cl set §a- Attempt to set current chunk as chunk loader.");
        sender.sendMessage("§2/cl delete §a- Remove chunk loader from current chunk.");
        sender.sendMessage("§2/cl delete <x> <z> [world] §a- Remove chunk loader chunk at chunk x, z in current world or another world.");
        sender.sendMessage("§2/cl bump §a- Resets timers for all chunk loaders."); // TODO: only display if bumping is enabled

        if (sender.hasPermission(TownyChunkLoader.PERMISSION_COMMAND_HERE))
            sender.sendMessage("§2/cl here §a- Check if there's a chunk loader in the current chunk and gives info about the chunk loader.");
        else
            sender.sendMessage("§2/cl here §a- Check if there's a chunk loader in the current chunk.");

        if (sender.hasPermission(TownyChunkLoader.PERMISSION_COMMAND_FORCEDELETE)) {
            sender.sendMessage("§2/cl forcedelete §a- Remove all chunk loaders on the server.");
            sender.sendMessage("§2/cl forcedelete world <world> §a- Remove all chunk loaders in the specified world.");
            sender.sendMessage("§2/cl forcedelete player <player> §a- Remove all chunk loaders owned by specified player.");
        }

        if (sender.hasPermission(TownyChunkLoader.PERMISSION_COMMAND_FORCEPROGRESS)) {
            sender.sendMessage("§2/cl forceprogress <seconds> §a- Force updates to current chunk as if it had been unloaded for X seconds.");
        }

        return true;
    }

}
