package org.josefadventures.core;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public class ExtendedPlayer {

    private static HashMap<UUID, ExtendedPlayer> instances = new HashMap<>();

    public static ExtendedPlayer from(UUID uuid) {
        if (instances.containsKey(uuid)) {
            return instances.get(uuid);
        } else {
            return new ExtendedPlayer(Bukkit.getPlayer(uuid), Bukkit.getOfflinePlayer(uuid));
        }
    }

    public static ExtendedPlayer from(Player player) {
        if (instances.containsKey(player.getUniqueId())) {
            return instances.get(player.getUniqueId());
        } else {
            return new ExtendedPlayer(player);
        }
    }

    public static ExtendedPlayer from(OfflinePlayer player) {
        if (instances.containsKey(player.getUniqueId())) {
            return instances.get(player.getUniqueId());
        } else {
            return new ExtendedPlayer(player);
        }
    }

    private Core plugin;

    private Player player;
    private OfflinePlayer offlinePlayer;

    private int databaseId;

    public Player getPlayer() {
        return this.player;
    }

    public OfflinePlayer getOfflinePlayer() {
        return this.offlinePlayer;
    }

    public UUID getUniqueId() {
        if (this.player != null) {
            return this.player.getUniqueId();
        } else {
            return this.offlinePlayer.getUniqueId();
        }
    }

    public String getUsername() {
        if (this.player != null) {
            return this.player.getName();
        } else {
            return this.offlinePlayer.getName();
        }
    }

    private int getDatabaseId() {
        return this.databaseId;
    }

    private ExtendedPlayer(Player player, OfflinePlayer offlinePlayer) {
        this.player = player;
        this.offlinePlayer = offlinePlayer;
        this.plugin = (Core) Bukkit.getServer().getPluginManager().getPlugin("Core");

        if (!this.existsInStorage()) {
            if (!this.createEntryInStorage()) {
                this.plugin.getLogger().warning("Could not create player entry in PostgreSQL.");
            }
        }
    }

    private ExtendedPlayer(OfflinePlayer player) {
        this(null, player);
    }

    private ExtendedPlayer(Player player) {
        this(player, null);
    }

    public boolean setString(String key, String value) {
        try {
            if (value == null) {
                PreparedStatement ps = Storage.getConnection().prepareStatement("DELETE FROM player_strings WHERE player_id = ? AND key = ?");
                ps.setInt(1, this.getDatabaseId());
                ps.setString(2, key);
                return ps.executeUpdate() > 0;
            } else {
                PreparedStatement ps = Storage.getConnection().prepareStatement("INSERT INTO player_strings (player_id, key, value) VALUES (?, ?, ?) ON CONFLICT (player_id, key) DO UPDATE SET value = ?");
                ps.setInt(1, this.getDatabaseId());
                ps.setString(2, key);
                ps.setString(3, value);
                ps.setString(4, value);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public String getString(String key) {
        try {
            PreparedStatement ps = Storage.getConnection().prepareStatement("SELECT value FROM player_strings WHERE player_id = ? AND key = ?");
            ps.setInt(1, this.getDatabaseId());
            ps.setString(2, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private boolean existsInStorage() {
        try {
            PreparedStatement ps = Storage.getConnection().prepareStatement("SELECT count(*) FROM players WHERE uuid = ?");
            ps.setString(1, this.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt(1) > 0) {
                    return true;
                }
            }
        } catch (SQLException | NullPointerException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private boolean createEntryInStorage() {
        try {
            PreparedStatement ps = Storage.getConnection().prepareStatement("INSERT INTO players (uuid, username) VALUES (?, ?)");
            ps.setString(1, this.getUniqueId().toString());
            ps.setString(2, this.getUsername());
            if (ps.execute()) {
                return true;
            }
        } catch (SQLException | NullPointerException ex) {
            ex.printStackTrace();
        }
        return false;
    }

}
