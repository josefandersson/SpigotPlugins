package org.josefadventures.townychunkloader.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.josefadventures.townychunkloader.TownyChunkLoader;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class CommandForceDelete {

    @SuppressWarnings("deprecation")
    public static boolean onCommand(CommandSender sender, Command cmd, String[] args) {
        if (!sender.hasPermission(TownyChunkLoader.PERMISSION_COMMAND_FORCEDELETE)) {
            sender.sendMessage(ChatColor.RED + cmd.getPermissionMessage());
            return true;
        }

        switch (args.length) {

            // Delete ALL chunk loaders
            // TODO: ADD A FUCKING FAIL SAFE, DON'T WANNA ACCIDENTALLY DO THIS!!
            case 1:
                try {
                    int num = TownyChunkLoader.instance.connection.createStatement().executeUpdate("DELETE FROM cl_chunks");

                    sender.sendMessage("§aRemoved all §2" + num + "§a chunk loader(s) from the server.");
                } catch (SQLException ex) {
                    TownyChunkLoader.instance.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
                    sender.sendMessage("§cSomething went wrong.");
                }

                return true;


            case 3:
                // Delete all in specified world
                // TODO: ADD A FUCKING FAIL SAFE, DON'T WANNA ACCIDENTALLY DO THIS!!
                if (args[1].equalsIgnoreCase("world")) {
                    World world = Bukkit.getWorld(args[2]);
                    if (world == null) {
                        sender.sendMessage("§cWorld §4" + args[2] + " §cnot found.");
                        return true;
                    }

                    try {
                        PreparedStatement ps = TownyChunkLoader.instance.connection.prepareStatement("DELETE FROM cl_chunks WHERE world_uuid = ?");
                        ps.setString(1, world.getUID().toString());
                        int num = ps.executeUpdate();

                        sender.sendMessage("§aRemoved §2" + num + "§a chunk loader(s) from world §2" + world.getName() + "§a.");
                    } catch (SQLException ex) {
                        TownyChunkLoader.instance.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
                        sender.sendMessage("§cSomething went wrong.");
                    }

                    return true;

                // Delete all by specified player
                // TODO: ADD A FUCKING FAIL SAFE, DON'T WANNA ACCIDENTALLY DO THIS!!
                } else if (args[1].equalsIgnoreCase("player")) {
                    String targetUuid;
                    String targetName;

                    Player targetPlayer = Bukkit.getPlayer(args[2]);
                    if (targetPlayer == null) {
                        OfflinePlayer targetOfflinePlayer = Bukkit.getOfflinePlayer(args[2]);
                        targetUuid = targetOfflinePlayer.getUniqueId().toString();
                        targetName = targetOfflinePlayer.getName();
                    } else {
                        targetUuid = targetPlayer.getUniqueId().toString();
                        targetName = targetPlayer.getDisplayName();
                    }

                    try {
                        PreparedStatement ps = TownyChunkLoader.instance.connection
                                .prepareStatement("DELETE FROM cl_chunks WHERE player_id = (SELECT id FROM cl_players WHERE uuid = ?)");
                        ps.setString(1, targetUuid);
                        int num = ps.executeUpdate();

                        sender.sendMessage("§aRemoved §2" + num + "§a chunk loader(s) from player §2" + targetName + "§a.");
                    } catch (SQLException ex) {
                        TownyChunkLoader.instance.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
                        sender.sendMessage("§cSomething went wrong.");
                    }

                    return true;
                }
        }

        sender.sendMessage("§cWrong usage of the command. Use §4/cl help §cto list all commands.");
        return true;
    }

}
