package org.josefadventures.townychunkloader;

import com.palmergames.bukkit.towny.event.TownUnclaimEvent;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.josefadventures.townychunkloader.command.*;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;

public class TownyChunkLoader extends JavaPlugin implements Listener {

    public static final String PERMISSION_COMMAND = "townychunkloader.command";
    public static final String PERMISSION_COMMAND_LIST_ALL = "townychunkloader.command.list.all";
    public static final String PERMISSION_COMMAND_LIST_PLAYER = "townychunkloader.command.list.player";
    public static final String PERMISSION_COMMAND_HERE = "townychunkloader.command.here";
    public static final String PERMISSION_COMMAND_FORCEDELETE = "townychunkloader.command.forcedelete";
    public static final String PERMISSION_COMMAND_FORCEPROGRESS = "townychunkloader.command.forceprogress";
    public static final String PERMISSION_CHUNKS_ = "townychunkloader.chunks.";
    public static final String PERMISSION_CHUNKS_EXEMPT = "townychunkloader.chunks.exempt";
    public static final String PERMISSION_TIME_ = "townychunkloader.time.";
    public static final String PERMISSION_TIME_EXEMPT = "townychunkloader.time.exempt"; // TODO:

    public static TownyChunkLoader instance;

    private static HashMap<String, String> worldNames = new HashMap<>();
    private static HashMap<String, String> playerNames = new HashMap<>();
    private static HashMap<String, Double> worldMultipliers = new HashMap<>();

    public Connection connection;

    private String conUrl = "jdbc:postgresql://host:port/database";
    private String conSchema = "townychunkloader";
    private String conUser = "";
    private String conPass = "";

    public int maxTimeHours = 168; // 1 week
    public double growthMultiplier = 1;
    public int maxUpdatesPerTick = 10;

    @Override
    public void onEnable() {
        instance = this;

        getConfig().addDefault("maxTimeHours", this.maxTimeHours);
        getConfig().addDefault("growthMultiplier", this.growthMultiplier);
        getConfig().addDefault("maxUpdatesPerTick", this.maxUpdatesPerTick);
        getConfig().addDefault("worlds", new String[] {}); // TODO: world specific multipliers?

        getConfig().addDefault("postgresql_url", this.conUrl);
        getConfig().addDefault("postgresql_schema", this.conSchema);
        getConfig().addDefault("postgresql_username", this.conUser);
        getConfig().addDefault("postgresql_password", this.conPass);
        getConfig().options().copyDefaults(true);
        saveConfig();

        this.maxTimeHours = getConfig().getInt("maxTimeHours");
        this.growthMultiplier = getConfig().getDouble("growthMultiplier");
        this.maxUpdatesPerTick = getConfig().getInt("maxUpdatesPerTick");

        this.conUrl = getConfig().getString("postgresql_url");
        this.conSchema = getConfig().getString("postgresql_schema");
        this.conUser = getConfig().getString("postgresql_username");
        this.conPass = getConfig().getString("postgresql_password");

        if (this.connectToDatabase()) {
            getLogger().info("Connected to PostgreSQL.");
        } else {
            getLogger().warning("Could not connect to PostgreSQL. Wrong credentials in config.yml?");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (this.createDefaultTables()) {
            getLogger().info("PostgreSQL database is setup.");
        } else {
            getLogger().warning("Could not setup PostgreSQL database. PostgreSQL user lacking permission?");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);

        ConfigurationSection cs = getConfig().getConfigurationSection("worlds");
        if (cs != null) {
            for (String worldName : cs.getKeys(false)) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    // TODO: do something
                    worldMultipliers.put(world.getUID().toString(), cs.getConfigurationSection(worldName).getDouble("growthMultiplier"));
                } else {
                    getLogger().warning("World with name \"" + worldName + "\" not found.");
                }
            }
        } else {
            getLogger().warning("No worlds defined in config.yml.");
        }

        try {
            PreparedStatement ps = this.connection.prepareStatement("INSERT INTO cl_players (uuid) VALUES (?) ON CONFLICT DO NOTHING");
            for (Player player : getServer().getOnlinePlayers()) {
                ps.setString(1, player.getUniqueId().toString());
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }

        // TODO: this doesn't make sense? why update chunks that are loaded?
        /*
        try {
            ResultSet rs = this.connection.createStatement().executeQuery("SELECT * FROM cl_chunks WHERE last_update < last_bump + ttl_millis");
            while (rs.next()) {
                World world = Bukkit.getWorld(UUID.fromString(rs.getString("world_uuid")));
                if (world != null && world.isChunkLoaded(rs.getInt("x"), rs.getInt("z"))) {
                    double interval = Math.min(rs.getDouble("last_bump") + rs.getDouble("ttl_millis") - rs.getDouble("last_update"),
                            rs.getDouble("ttl_millis"));
                    int ticks = (int) interval / 50;
                    this.progressChunk(world.getChunkAt(rs.getInt("x"), rs.getInt("z")), ticks, rs.getDouble("growth_multiplier")); // TODO: player multiplier
                    // TODO: update last_update
                }
            }
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }*/
    }

    @Override
    public void onDisable() {
        try {
            this.connection.close();
            getLogger().info("Closed PostgreSQL connection.");
        } catch (SQLException | NullPointerException ex) { }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION_COMMAND)) {
            sender.sendMessage(ChatColor.DARK_RED + cmd.getPermissionMessage());
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            return CommandHelp.onCommand(sender);
        }

        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "l":
            case "list":
                return CommandList.onCommand(sender, cmd, args);

            case "fd":
            case "fr":
            case "forced":
            case "forcer":
            case "forcedel":
            case "forceremove":
            case "forcedelete":
                return CommandForceDelete.onCommand(sender, cmd, args);
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Either your command was unrecognized or it cannot be used in console.");
            return true;
        }

        Player player = (Player) sender;

        switch (subCmd) {
            case "s":
            case "set":
                return CommandSet.onCommand(player);

            case "d":
            case "del":
            case "delete":
            case "remove":
                return CommandDelete.onCommand(player, args);

            case "b":
            case "bump":
                return CommandBump.onCommand(player);

            case "h":
            case "here":
                return CommandHere.onCommand(player);

            case "fp":
            case "forcep":
            case "forceprogress":
                return CommandForceProgress.onCommand(player, cmd, args);
        }

        sender.sendMessage("§cCouldn't recognize your command. Use §4/cl help §cto list all commands.");
        return true;
    }

    /**
     * Progresses a chunk (grows, decays) X game ticks. Adds multipliers from server and world.
     * @param chunk chunk to progress.
     * @param numTicks number of games ticks to progress. (20 ticks = 1 second)
     * @param multiplier increase or decrease chance of random tick.
     */
    public void progressChunk(Chunk chunk, int numTicks, double multiplier) {
        double worldMultiplier = 1;
        if (worldMultipliers.containsKey(chunk.getWorld().getUID().toString())) {
            worldMultiplier = worldMultipliers.get(chunk.getWorld().getUID().toString());
        }

        System.out.println("Progressing chunk with multiplier: " + multiplier * worldMultiplier * this.growthMultiplier);

        new ChunkProgressor(chunk, numTicks, multiplier * worldMultiplier * this.growthMultiplier);
    } // TODO: add a queue for chunkprogressors, so that only one runs at a time

    @EventHandler
    public void onPlayerLoginEvent(PlayerLoginEvent ev) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                PreparedStatement ps = this.connection.prepareStatement("INSERT INTO cl_players (uuid) VALUES (?) ON CONFLICT DO NOTHING");
                ps.setString(1, ev.getPlayer().getUniqueId().toString());
                ps.executeUpdate();

                ps = this.connection.prepareStatement("SELECT count(*) AS num FROM cl_chunks WHERE last_bump + ttl_millis < ? AND player_id = (SELECT id FROM cl_players WHERE uuid = ?)");
                ps.setLong(1, System.currentTimeMillis());
                ps.setString(2, ev.getPlayer().getUniqueId().toString());
                ResultSet rs = ps.executeQuery();
                rs.next();

                int num = rs.getInt("num");
                if (num > 0) {
                    if (num == 1) {
                        ev.getPlayer().sendMessage("§bYou have §91 §bchunk loader that need bumping! Use §b/cl bump §9 to bump it.");
                    } else {
                        ev.getPlayer().sendMessage("§bYou have §9" + num + " §bchunk loaders that need bumping! Use §b/cl bump §9 to bump them.");
                    }
                }
            } catch (SQLException ex) {
                getLogger().log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent ev) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                PreparedStatement ps = this.connection
                        .prepareStatement("SELECT * FROM cl_chunks WHERE world_uuid = ? AND x = ? AND z = ? AND last_update < last_bump + ttl_millis",
                                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
                ps.setString(1, ev.getChunk().getWorld().getUID().toString());
                ps.setInt(2, ev.getChunk().getX());
                ps.setInt(3, ev.getChunk().getZ());
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    double interval = Math.min(rs.getDouble("last_bump") + rs.getDouble("ttl_millis") - rs.getDouble("last_update"),
                            rs.getDouble("ttl_millis"));
                    int ticks = (int) interval / 50;
                    this.progressChunk(ev.getChunk(), ticks, rs.getDouble("growth_multiplier")); // TODO: player multiplier

                    System.out.println("progressing a loaded chunks by ticks " + ticks);

                    rs.updateLong("last_update", System.currentTimeMillis());
                    rs.updateRow();
                }

                rs.close();
            } catch (SQLException ex) {
                getLogger().log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent ev) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                PreparedStatement ps = this.connection.prepareStatement("UPDATE cl_chunks SET last_update = ? WHERE world_uuid = ? AND x = ? AND z = ?");
                ps.setLong(1, System.currentTimeMillis());
                ps.setString(2, ev.getWorld().getUID().toString());
                ps.setInt(3, ev.getChunk().getX());
                ps.setInt(4, ev.getChunk().getZ());
                ps.executeUpdate();
            } catch (SQLException ex) {
                getLogger().log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

    @EventHandler
    public void onTownUnclaim(TownUnclaimEvent ev) {

    } // TODO: this is important





    public static String worldName(String uuid) {
        if (!worldNames.containsKey(uuid)) {
            World world = Bukkit.getWorld(UUID.fromString(uuid));
            if (world != null) {
                worldNames.put(uuid, "§2" + world.getName());
            } else {
                worldNames.put(uuid, "§cUNKNOWN");
            }
        }
        return worldNames.get(uuid);
    }

    public static String playerName(String uuid) {
        if (!playerNames.containsKey(uuid)) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            playerNames.put(uuid, offlinePlayer.getName());
        }
        return playerNames.get(uuid);
    }

    public int countChunkLoaders(UUID ownerUniqueId) {
        try {
            PreparedStatement ps = this.connection.prepareStatement("SELECT count(*) AS num FROM cl_chunks WHERE player_id = (SELECT id FROM cl_players WHERE uuid = ?)");
            ps.setString(1, ownerUniqueId.toString());
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt("num");
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, ex.getMessage(), ex);
            return -1;
        }
    }

    private boolean connectToDatabase() {
        try {
            this.connection = DriverManager.getConnection(this.conUrl, this.conUser, this.conPass);
            this.connection.setSchema(this.conSchema);
            return true;
        } catch (SQLException ex) { }
        return false;
    }

    private boolean createDefaultTables() {
        try {
            String schema = getConfig().getString("postgresql_schema");
            String username = getConfig().getString("postgresql_username");

            // cl_players - id, uuid, max_chunks=-1, growth_multiplier=1
            // cl_chunks - id (world_id:x:y), world_id, player_id, x, z, last_bump, last_updated, growth_multiplier=1, ttl_hours=4

            /*
             * ttl_millis (max time to progress)
             * last_bump
             *
             * SELECT
             * progress_interval = LEAST (last_bump + ttl_millis - last_update, ttl_millis)
             */

            Statement statement = this.connection.createStatement();
            statement.addBatch("CREATE SCHEMA IF NOT EXISTS " + schema + " AUTHORIZATION " + username);
            statement.addBatch("CREATE TABLE IF NOT EXISTS cl_players ( id SERIAL UNIQUE, uuid VARCHAR UNIQUE NOT NULL, max_chunks INT DEFAULT -1, PRIMARY KEY(id, uuid) )");
            statement.addBatch("CREATE TABLE IF NOT EXISTS cl_chunks ( id SERIAL, world_uuid VARCHAR NOT NULL, player_id INT REFERENCES cl_players(id) ON DELETE CASCADE, " +
                    "x INT NOT NULL, z INT NOT NULL, last_bump FLOAT8 DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000, last_update FLOAT8 DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000, " +
                    "growth_multiplier FLOAT4 DEFAULT 1.0, ttl_millis FLOAT8 DEFAULT 14400000, PRIMARY KEY(id, world_uuid, x, z) )");
            statement.executeBatch();

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

}
