package org.josefadventures.townychunkloader.command;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.josefadventures.townychunkloader.TownyChunkLoader;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

public class CommandList {

    @SuppressWarnings("deprecation")
    public static boolean onCommand(CommandSender sender, Command cmd,  String[] args) {
        switch (args.length) {

            // List own chunk loaders
            case 1:
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only '/cl list all' and '/cl list player <player>' can be used in the console.");
                    return true;
                }

                try {
                    PreparedStatement ps = TownyChunkLoader.instance.connection.prepareStatement("SELECT * FROM cl_chunks WHERE player_id = (SELECT id FROM cl_players WHERE uuid = ?)");
                    ps.setString(1, ((Player) sender).getUniqueId().toString());
                    ResultSet rs = ps.executeQuery();

                    sender.sendMessage("§aYour chunk loaders:");
                    int count = 0;

                    while (rs.next()) {
                        sender.sendMessage("§a#§2" + (count+++1) + " §a[" + TownyChunkLoader.worldName(rs.getString("world_uuid")) + "§a] (x:§2"
                                + rs.getInt("x") + "§a z:§2" + rs.getInt("z") + "§a): "); // TODO: add more info
                    }
                    sender.sendMessage("§a(§2" + count + " §ain total)");
                } catch (SQLException ex) {
                    TownyChunkLoader.instance.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
                    sender.sendMessage("§cSomething went wrong.");
                }

                return true;

            // List all chunk loaders
            case 2:
                if (args[1].equalsIgnoreCase("all")) {
                    if (sender.hasPermission(TownyChunkLoader.PERMISSION_COMMAND_LIST_ALL)) {
                        try {
                            ResultSet rs = TownyChunkLoader.instance.connection.createStatement()
                                    .executeQuery("SELECT *, cl_players.uuid AS player_uuid FROM cl_chunks JOIN cl_players ON cl_players.id = player_id"); // TODO: untested statement

                            sender.sendMessage("§aAll chunk loaders on the server:");
                            int count = 0;

                            while (rs.next()) {
                                sender.sendMessage("§a#§2" + (count++ + 1) + " §a[" + TownyChunkLoader.worldName(rs.getString("world_uuid")) + "§a] (x:§2"
                                        + rs.getInt("x") + "§a z:§2" + rs.getInt("z") + "§a) {§2"
                                        + TownyChunkLoader.playerName(rs.getString("player_uuid")) + "§a}: "); // TODO: add more info
                            }
                            sender.sendMessage("§a(§2" + count + " §ain total)");
                        } catch (SQLException ex) {
                            TownyChunkLoader.instance.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
                            sender.sendMessage("§cSomething went wrong.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.DARK_RED + cmd.getPermissionMessage());
                    }

                    return true;
                }
                break;

            // List a players chunk loaders
            case 3:
                if (args[1].equalsIgnoreCase("player")) {
                    if (sender.hasPermission(TownyChunkLoader.PERMISSION_COMMAND_LIST_PLAYER)) {
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
                                    .prepareStatement("SELECT * FROM cl_chunks WHERE player_id = (SELECT id FROM cl_players WHERE uuid = ?)");
                            ps.setString(1, targetUuid);
                            ResultSet rs = ps.executeQuery();

                            sender.sendMessage("§2" + targetName + "§a's chunk loaders:");
                            int count = 0;

                            while (rs.next()) {
                                sender.sendMessage("§a#§2" + (count+++1) + " §a[" + TownyChunkLoader.worldName(rs.getString("world_uuid")) + "§a] (x:§2"
                                        + rs.getInt("x") + "§a z:§2" + rs.getInt("z") + "§a): "); // TODO: add more info
                            }
                            sender.sendMessage("§a(§2" + count + " §ain total)");
                        } catch (SQLException ex) {
                            TownyChunkLoader.instance.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
                            sender.sendMessage("§cSomething went wrong.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.DARK_RED + cmd.getPermissionMessage());
                    }

                    return true;
                }
                break;
        }

        sender.sendMessage("§cWrong usage of the command. Use §4/cl help §cto list all commands.");
        return true;
    }


}
