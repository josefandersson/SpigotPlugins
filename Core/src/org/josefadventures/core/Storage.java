package org.josefadventures.core;

import org.bukkit.Bukkit;

import java.sql.*;
import java.util.logging.Level;

public class Storage {

    private static Connection connection;

    private static String conUrl;
    private static String conUser;
    private static String conPass;
    private static String conSchema;

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not load PostgreSQL driver! Probably something wrong with the Core plugin!", ex);
        }
    }

    public static Connection getConnection() {
        if (isConnected()) {
            return connection;
        }
        return  null;
    }

    public static void setup(String url, String schema, String username, String password) {
        conUrl = url;
        conSchema = schema;
        conUser = username;
        conPass = password;
    }

    public static boolean connect() {
        if (conUrl != null && conUser != null && conPass != null) {
            return connect(conUrl, conSchema, conUser, conPass);
        }
        return false;
    }

    public static boolean connect(String url, String schema, String username, String password) {
        if (isConnected()) {
            return true;
        }

        try {
            connection = DriverManager.getConnection(url, username, password);
            connection.setSchema(schema);
        } catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public static boolean isConnected() {
        try {
            return !connection.isClosed();
        } catch (SQLException | NullPointerException ex) {
            return false;
        }
    }

}
