package org.josefadventures.townychunkloader.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.josefadventures.townychunkloader.TownyChunkLoader;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class CommandHere {

    public static boolean onCommand(Player player) {
        try {
            PreparedStatement ps = TownyChunkLoader.instance.connection
                    .prepareStatement("SELECT *, cl_players.uuid AS player_uuid FROM cl_chunks JOIN cl_players ON cl_players.id = cl_chunks.player_id WHERE world_uuid = ? AND x = ? AND z = ?");
            ps.setString(1, player.getLocation().getWorld().getUID().toString());
            ps.setInt(2, player.getLocation().getChunk().getX());
            ps.setInt(3, player.getLocation().getChunk().getZ());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(rs.getString("player_uuid")));
                if (player.getUniqueId().equals(offlinePlayer.getUniqueId())) {
                    player.sendMessage("§a=== §2Chunk Loader §a(world:§2" + player.getLocation().getWorld().getName() + " §ax:§2" + rs.getInt("x") + " §az:§2" + rs.getInt("z") + "§a) ===");
                    player.sendMessage("§aOwner: §2" + offlinePlayer.getName() + " §a(you)");
                    player.sendMessage("§aLast Bump: §2" + String.format("%.2f", ((System.currentTimeMillis() - rs.getLong("last_bump")) / 3600000d)) + " §ahours ago.");
                    player.sendMessage("§aLast Update: §2" + String.format("%.2f", ((System.currentTimeMillis() - rs.getLong("last_update")) / 3600000d)) + " §ahours ago.");
                    // TODO: add more info
                    player.sendMessage("§a=== §2/Chunk Loader §a===");
                } else {
                    if (player.hasPermission(TownyChunkLoader.PERMISSION_COMMAND_HERE)) {
                        player.sendMessage("§a=== §2Chunk Loader §a(world:§2" + player.getLocation().getWorld().getName() + " §ax:§2" + rs.getInt("x") + " §az:§2" + rs.getInt("z") + "§a) ===");
                        player.sendMessage("§aOwner: §2" + offlinePlayer.getName());
                        player.sendMessage("§aLast Bump: §2" + String.format("%.2f", ((System.currentTimeMillis() - rs.getLong("last_bump")) / 3600000d)) + " §ahours ago.");
                        player.sendMessage("§aLast Update: §2" + String.format("%.2f", ((System.currentTimeMillis() - rs.getLong("last_update")) / 3600000d)) + " §ahours ago.");
                        // TODO: add more info
                        player.sendMessage("§a=== §2/Chunk Loader §a===");
                    } else {
                        player.sendMessage("§aThis chunk has a chunk loader.");
                    }
                }
            } else {
                player.sendMessage("§aThis chunk does not have a chunk loader.");
            }
        } catch (SQLException ex) {
            TownyChunkLoader.instance.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
            player.sendMessage("§cSomething went wrong.");
        }
        return true;
    }

}
