package org.josefadventures.townychunkloader.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.josefadventures.townychunkloader.TownyChunkLoader;

public class CommandForceProgress {

    public static boolean onCommand(Player player, Command cmd, String[] args) {
        if (!player.hasPermission(TownyChunkLoader.PERMISSION_COMMAND_FORCEDELETE)) {
            player.sendMessage(ChatColor.RED + cmd.getPermissionMessage());
            return true;
        }

        if (args.length == 2) {
            try {
                int seconds = Integer.parseInt(args[1]);
                int ticks = seconds * 20;

                // TODO: get chunk loader, apply player multiplier if relevant, chunk multiplier
                // TODO: add argument for extra multiplier on top of standards

                TownyChunkLoader.instance.progressChunk(player.getLocation().getChunk(), ticks, 1);

                player.sendMessage("§aProgressing current chunk §2" + ticks + " §aticks.");
                return true;
            } catch (NumberFormatException ex) {
                player.sendMessage("§cThe second argument has to be a number. Use §4/cl help §cfor usage help.");
                return true;
            }
        }

        player.sendMessage("§cWrong usage of the command. Use §4/cl help §cto list all commands.");
        return true;
    }

}
