package org.josefadventures.core;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_13_R2.CraftOfflinePlayer;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.logging.Level;

public class Core extends JavaPlugin {

    static String url = "jdbc:postgresql://Olympos:5432/test";
    static String user = "test";
    static String pass = "test";

    @Override
    public void onEnable() {
        getConfig().addDefault("postgresql_url", "jdbc:postgresql://host:port/database");
        getConfig().addDefault("postgresql_schema", "core");
        getConfig().addDefault("postgresql_username", "");
        getConfig().addDefault("postgresql_password", "");
        getConfig().options().copyDefaults(true);

        saveConfig();

        if (Storage.connect(
                getConfig().getString("postgresql_url"),
                getConfig().getString("postgresql_schema"),
                getConfig().getString("postgresql_username"),
                getConfig().getString("postgresql_password"))) {
            getLogger().info("Connected to PostgreSQL database.");
        } else {
            getLogger().warning("Could not connect to PostgreSQL database. Make sure the credentials in config.yml are correct.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!this.createDefaultTables()) {
            getLogger().warning("Could not create default tables in PostgreSQL database. Does the account have the right permissions?");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (ExtendedPlayer.from(Bukkit.getOfflinePlayer("DrDoof")).setString("TESTTTT", "hehehenigger")) {
            System.out.println("SAVED!");
        }
    }

    private boolean createDefaultTables() {
        try {
            String schema = getConfig().getString("postgresql_schema");
            String username = getConfig().getString("postgresql_username");

            Statement statement = Storage.getConnection().createStatement();
            statement.addBatch("CREATE SCHEMA IF NOT EXISTS " + schema + " AUTHORIZATION " + username);
            statement.addBatch("CREATE TABLE IF NOT EXISTS players ( id SERIAL, uuid VARCHAR NOT NULL, username VARCHAR NOT NULL, last_update TIMESTAMP DEFAULT NOW(), PRIMARY KEY(id) )");
            statement.addBatch("CREATE TABLE IF NOT EXISTS player_strings ( player_id INTEGER REFERENCES players(id) ON DELETE CASCADE, key VARCHAR NOT NULL, value VARCHAR NOT NULL, PRIMARY KEY(player_id, key) )");
            statement.addBatch("CREATE TABLE IF NOT EXISTS player_ints ( player_id INTEGER REFERENCES players(id) ON DELETE CASCADE, key VARCHAR NOT NULL, value INT NOT NULL, PRIMARY KEY(player_id, key) )");
            statement.addBatch("CREATE OR REPLACE FUNCTION update_last_update() RETURNS TRIGGER AS $$ BEGIN NEW.last_update = NOW(); RETURN NEW; END; $$ LANGUAGE 'plpgsql'");
            statement.addBatch("CREATE TRIGGER update_last_update_on_players BEFORE UPDATE ON players FOR EACH ROW EXECUTE PROCEDURE update_last_update()");
            statement.executeBatch();

            // Storage.getConnection().createStatement().execute("CREATE SCHEMA IF NOT EXISTS " + schema + " AUTHORIZATION " + username);
            // Storage.getConnection().createStatement().execute("CREATE TABLE IF NOT EXISTS players ( id SERIAL, uuid VARCHAR NOT NULL, username VARCHAR NOT NULL, last_update TIMESTAMP DEFAULT NOW(), PRIMARY KEY(id) )");
            // Storage.getConnection().createStatement().execute("CREATE TABLE IF NOT EXISTS player_strings ( player_id INTEGER REFERENCES players(id) ON DELETE CASCADE, key VARCHAR NOT NULL, value VARCHAR NOT NULL, PRIMARY KEY(player_id, key) )");
            // Storage.getConnection().createStatement().execute("CREATE TABLE IF NOT EXISTS player_ints ( player_id INTEGER REFERENCES players(id) ON DELETE CASCADE, key VARCHAR NOT NULL, value INT NOT NULL, PRIMARY KEY(player_id, key) )");

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
