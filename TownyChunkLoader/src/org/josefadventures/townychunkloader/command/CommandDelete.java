package org.josefadventures.townychunkloader.command;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.josefadventures.townychunkloader.TownyChunkLoader;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

public class CommandDelete {

    public static boolean onCommand(Player player, String[] args) {
        switch (args.length) {

            // Delete chunk loader here
            case 1:
                try {
                    PreparedStatement ps = TownyChunkLoader.instance.connection
                            .prepareStatement("SELECT *, cl_players.uuid AS player_uuid FROM cl_chunks JOIN cl_players ON cl_players.id = cl_chunks.player_id WHERE world_uuid = ? AND x = ? AND z = ?",
                                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
                    ps.setString(1, player.getLocation().getWorld().getUID().toString());
                    ps.setInt(2, player.getLocation().getChunk().getX());
                    ps.setInt(3, player.getLocation().getChunk().getZ());
                    ResultSet rs = ps.executeQuery();

                    if (attemptDelete(player, rs)) {
                        player.sendMessage("§aYou removed the chunk loader from this chunk.");
                    }

                    rs.close();
                    return true;
                } catch (SQLException ex) {
                    TownyChunkLoader.instance.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
                    player.sendMessage("§cSomething went wrong.");
                    return true;
                }

            // Delete chunk loader at x, z[, world]
            case 3:
            case 4:
                try {
                    int x = Integer.parseInt(args[1]);
                    int z = Integer.parseInt(args[2]);
                    String worldUuid;
                    String worldName;

                    if (args.length == 4) {
                        World world = Bukkit.getWorld(args[3]);
                        if (world != null) {
                            worldUuid = world.getUID().toString();
                            worldName = world.getName();
                        } else {
                            player.sendMessage("§cWorld §4" + args[3] + " §cnot found.");
                            return true;
                        }
                    } else {
                        worldUuid = player.getLocation().getWorld().getUID().toString();
                        worldName = player.getWorld().getName();
                    }

                    PreparedStatement ps = TownyChunkLoader.instance.connection
                            .prepareStatement("SELECT *, cl_players.uuid AS player_uuid FROM cl_chunks JOIN cl_players ON cl_players.id = cl_chunks.player_id WHERE world_uuid = ? AND x = ? AND z = ?",
                                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
                    ps.setString(1, worldUuid);
                    ps.setInt(2, x);
                    ps.setInt(3, z);
                    ResultSet rs = ps.executeQuery();

                    if (attemptDelete(player, rs)) {
                        player.sendMessage("§aYou removed the chunk loader from chunk at x:§2" + x + " §az:§2" + z + " §ain world §2" + worldName + "§a.");
                    }

                    rs.close();
                    return true;
                } catch (SQLException ex) {
                    TownyChunkLoader.instance.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
                    player.sendMessage("§cSomething went wrong.");
                    return true;
                } catch (NumberFormatException ex) {
                    player.sendMessage("§cArgument §4x §cand §4z has to be numbers.");
                    return true;
                }
        }

        player.sendMessage("§cWrong usage of the command. Use §4/cl help §cto list all commands.");
        return true;
    }

    private static boolean attemptDelete(Player player, ResultSet rs) throws SQLException {
        if (rs.next()) {
            if (rs.getString("player_uuid").equals(player.getUniqueId().toString())) {
                rs.deleteRow();
                return true;
            } else {
                if (player.hasPermission(TownyChunkLoader.PERMISSION_COMMAND_HERE)) {
                    player.sendMessage("§cYou cannot remove the chunk loader on this chunk because it was created by the player §4"
                            + TownyChunkLoader.playerName(rs.getString("player_uuid")) + "§c.");
                } else {
                    player.sendMessage("§cYou cannot remove the chunk loader on this chunk because it was created by another player.");
                }
            }
        } else {
            player.sendMessage("§cCouldn't remove chunk loader because the chunk is not a chunk loader.");
        }
        return false;
    }

}
