package org.josefadventures.townychunkloader.command;

import org.bukkit.entity.Player;
import org.josefadventures.townychunkloader.TownyChunkLoader;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class CommandBump {

    public static boolean onCommand(Player player) {
        try {
            PreparedStatement ps = TownyChunkLoader.instance.connection.prepareStatement("UPDATE cl_chunks SET last_bump = ? WHERE player_id = (SELECT cl_players.id FROM cl_players WHERE uuid = ?)");
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, player.getUniqueId().toString());
            int num = ps.executeUpdate();

            if (num == 0) {
                player.sendMessage("§aYou have no chunk loaders to bump.");
            } else if (num == 1) {
                player.sendMessage("§aYou bumped one chunk loader.");
            } else {
                player.sendMessage("§aYou bumped §2" + num + " §achunk loaders.");
            }
        } catch (SQLException ex) {
            TownyChunkLoader.instance.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
            player.sendMessage("§cSomething went wrong.");
        }

        return true;
    }

}
