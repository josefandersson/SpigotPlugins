package org.josefadventures.townychunkloader.command;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import org.bukkit.entity.Player;
import org.josefadventures.townychunkloader.TownyChunkLoader;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

public class CommandSet {

    public static boolean onCommand(Player player) {
        if (!player.hasPermission(TownyChunkLoader.PERMISSION_CHUNKS_EXEMPT)) {
            int num = TownyChunkLoader.instance.countChunkLoaders(player.getUniqueId());

            for (int i = num; i >= 0; i--) {
                if (player.hasPermission(TownyChunkLoader.PERMISSION_CHUNKS_ + i)) {
                    if (num == 1)
                        player.sendMessage("§cYou are only allowed to set §41 §cchunk loader. Delete it with §4/cl d §cand try again.");
                    else
                        player.sendMessage("§cYou are only allowed to set §4" + num + " §cchunk loaders. Delete one with §4/cl d §cand try again.");
                    return true;
                }
            }
        }

        try {
            PreparedStatement ps = TownyChunkLoader.instance.connection
                    .prepareStatement("SELECT *, cl_players.uuid AS player_uuid FROM cl_chunks JOIN cl_players ON cl_players.id = cl_chunks.player_id WHERE world_uuid = ? AND x = ? AND z = ?");
            ps.setString(1, player.getLocation().getWorld().getUID().toString());
            ps.setInt(2, player.getLocation().getChunk().getX());
            ps.setInt(3, player.getLocation().getChunk().getZ());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getString("player_uuid").equals(player.getUniqueId().toString())) {
                    player.sendMessage("§cYou have already set this chunk as a chunk loader.");
                } else {
                    if (player.hasPermission(TownyChunkLoader.PERMISSION_COMMAND_HERE)) {
                        player.sendMessage("§4" + TownyChunkLoader.playerName(rs.getString("player_uuid")) + "§c has already set this chunk as a chunk loader.");
                    } else {
                        player.sendMessage("§cThis chunk already has a chunk loader.");
                    }
                }
                return true;
            }
        } catch (SQLException ex) {
            TownyChunkLoader.instance.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
            player.sendMessage("§cSomething went wrong.");
            return true;
        }

        Resident resident = null;
        Town town = null;
        try {
            resident = TownyUniverse.getDataSource().getResident(player.getName());
            town = TownyUniverse.getTownBlock(player.getLocation()).getTown();

            // TODO: create a config for only on players own town block and allowance to set chunk loaders in embassy's?

            if (!resident.getTown().equals(town)) {
                player.sendMessage("§cYou can only set a chunk loader in your own town.");
                return true;
            }
        } catch (Exception e) {
            player.sendMessage("§cYou can not set a chunk loader in the wild.");
            return true;
        }

        // TODO: fix this shit, default may be this, but there has to be a bool for knowing when ttl is discardable
        int hours = 876582; // default 100 years lol
        if (!player.hasPermission(TownyChunkLoader.PERMISSION_TIME_EXEMPT)) {
            hours = TownyChunkLoader.instance.maxTimeHours;
            for (; hours > 0; hours--) {
                if (player.hasPermission(TownyChunkLoader.PERMISSION_TIME_ + hours)) {
                    break;
                }
            }
            if (hours == 0) hours = TownyChunkLoader.instance.maxTimeHours;
        }

        try {
            PreparedStatement ps = TownyChunkLoader.instance.connection
                    .prepareStatement("INSERT INTO cl_chunks (player_id, world_uuid, x, z, ttl_millis) VALUES ((SELECT id FROM cl_players WHERE uuid = ?), ?, ?, ?, ?)");
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, player.getWorld().getUID().toString());
            ps.setInt(3, player.getLocation().getChunk().getX());
            ps.setInt(4, player.getLocation().getChunk().getZ());
            ps.setLong(5, hours * 3600000);
            if (ps.executeUpdate() > 0) {
                if (hours > 0) {
                    if (hours == 1)
                        player.sendMessage("§aYou set this chunk as a chunk loader. It will need to be bumped every hour.");
                    else
                        player.sendMessage("§aYou set this chunk as a chunk loader. It will need to be bumped every §2" + hours + "§a hours.");
                } else {
                    player.sendMessage("§aYou set this chunk as a chunk loader.");

                }
            } else {
                TownyChunkLoader.instance.getLogger().warning("Chunk loaded was not created but there was no SQL error?");
                player.sendMessage("§cSomething went wrong.");
            }
        } catch (SQLException ex) {
            TownyChunkLoader.instance.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
            player.sendMessage("§cSomething went wrong.");
        }

        return true;
    }
}
