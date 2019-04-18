package org.josefadventures.postgresql;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class PostgreSQL extends JavaPlugin {

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not load PostgreSQL driver!", ex);
        }
    }

}
