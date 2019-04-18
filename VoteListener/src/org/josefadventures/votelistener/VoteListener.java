package org.josefadventures.votelistener;

import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Level;

public class VoteListener extends JavaPlugin implements Listener {

    // TODO: fix rewards!! commands should be able to run from either console or voter (as temp OP) and commands should be able to be put on a timer (database trigger) (ie permission nodes disappearing after 24h)

    public static VoteListener instance;

    private Connection connection;

    private String conUrl = "jdbc:postgresql://host:port/database";
    private String conSchema = "votelistener";
    private String conUser = "";
    private String conPass = "";

    public String broadcastVotedMessage = "§6Someone voted for the server and got §c$25§6! Use §c/vote §6and do the same.";
    public String playerRewardMessage = "§6You voted and got §c$25§6!";
    public String playerLostRewardMessage;
    public String playerNotifyMessage;

    public List<String> commands;

    @Override
    public void onEnable() {
        instance = this;

        this.getConfig().addDefault("broadcastVotedMessage", this.broadcastVotedMessage);
        this.getConfig().addDefault("playerRewardMessage", this.playerRewardMessage);
        this.getConfig().addDefault("playerLostRewardMessage", this.playerLostRewardMessage);
        this.getConfig().addDefault("playerNotifyMessage", this.playerNotifyMessage);
        this.getConfig().addDefault("commands", new String[] {});

        getConfig().addDefault("postgresql_url", this.conUrl);
        getConfig().addDefault("postgresql_schema", this.conSchema);
        getConfig().addDefault("postgresql_username", this.conUser);
        getConfig().addDefault("postgresql_password", this.conPass);
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();

        this.broadcastVotedMessage = this.getConfig().getString("broadcastVotedMessage");
        this.playerRewardMessage = this.getConfig().getString("playerRewardMessage");
        this.playerLostRewardMessage = this.getConfig().getString("playerLostRewardMessage");
        this.playerNotifyMessage = this.getConfig().getString("playerNotifyMessage");
        this.commands = this.getConfig().getStringList("commands");

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
            getLogger().warning("Could not setup PostgreSQL database.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        try {
            this.connection.close();
        } catch (SQLException | NullPointerException ex) { }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (label) {
            case "vote":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cThis commmand is only for online users!");
                    return true;
                }

                sender.sendMessage("§Vote and receive rewards! (Green links = can vote, red links = already voted today.)");
                try {
                    PreparedStatement ps = this.connection
                            .prepareStatement("SELECT DISTINCT ON (name) name, url, interval, votes.timestamp AS timestamp " +
                                    "FROM vote_services " +
                                    "LEFT JOIN votes ON service_id = vote_services.id AND player_id = (SELECT id FROM vote_players WHERE uuid = ?) " +
                                    "ORDER BY name, votes.timestamp DESC");
                    ps.setString(1, ((Player) sender).getUniqueId().toString());
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {

                        Timestamp timestamp = rs.getTimestamp("timestamp");
                        if (timestamp == null || timestamp.toInstant().plus(rs.getInt("interval"), ChronoUnit.HOURS).isBefore(Instant.now())) {
                            sender.sendMessage("§2§n" + rs.getString("name") + " §r§2- §n" + rs.getString("url"));
                        } else {
                            sender.sendMessage("§c§n" + rs.getString("name") + " §r§c- §n" + rs.getString("url"));
                        }
                    }
                } catch (SQLException ex) {
                    getLogger().log(Level.SEVERE, ex.getMessage(), ex);
                    sender.sendMessage("§cSomething went wrong.");
                }
                return true;
            case "votelistener":
                if (args.length == 0) {
                    sender.sendMessage("§cNot enough arguments. Use §n/votelistener help §r§cfor a list of commands.");
                    return true;
                }

                switch (args[0].toLowerCase()) {
                    case "help":
                        sender.sendMessage("List of commands:");
                        sender.sendMessage("/votelistener services <list|add|remove>");
                        sender.sendMessage("/votelistener fakevote <username> <service_name>");
                        return true;
                    case "services":
                        if (args.length == 1) {
                            sender.sendMessage("§cSpecify either list, set or remove as the second argument.");
                            return true;
                        }

                        switch (args[1].toLowerCase()) {
                            case "list":
                                sender.sendMessage("§2List of services:");
                                try {
                                    ResultSet rs = this.connection.createStatement().executeQuery("SELECT * FROM vote_services");
                                    while (rs.next()) {
                                        sender.sendMessage("§2§n" + rs.getString("name") + " §r§2- §n" + rs.getString("url"));
                                    }
                                } catch (SQLException ex) {
                                    getLogger().log(Level.SEVERE, ex.getMessage(), ex);
                                    sender.sendMessage("§cSomething went wrong.");
                                }
                                return true;
                            case "set":
                                if (args.length  < 4) {
                                    sender.sendMessage("§cSpecify name of the service to set and it's url. (cAsE mAtTeRs)");
                                    return true;
                                }

                                try {
                                    String name = args[2];
                                    String url = args[3];
                                    boolean alreadyExists = false;

                                    PreparedStatement ps = this.connection.prepareStatement("SELECT * FROM vote_services WHERE name = ?");
                                    ps.setString(1, name);
                                    if (ps.executeQuery().next()) {
                                        alreadyExists = true;
                                    }

                                    if (args.length == 4) {
                                        ps = this.connection.prepareStatement("INSERT INTO vote_services (name, url) VALUES (?, ?) ON CONFLICT (name) DO UPDATE SET url = ?");
                                        ps.setString(3, url);
                                    } else {
                                        int interval = Integer.parseInt(args[4]);
                                        ps = this.connection.prepareStatement("INSERT INTO vote_services (name, url, interval) VALUES (?, ?, ?) ON CONFLICT (name) DO UPDATE SET url = ?, interval = ?");
                                        ps.setInt(3, interval);
                                        ps.setString(4, url);
                                        ps.setInt(5, interval);
                                    }

                                    ps.setString(1, name);
                                    ps.setString(2, url);
                                    ps.execute();

                                    if (alreadyExists)
                                        sender.sendMessage("§2Successfully updated service §n" + name + " §r§2.");
                                    else
                                        sender.sendMessage("§2Successfully added service §n" + name + " §r§2.");
                                } catch (SQLException ex) {
                                    getLogger().log(Level.SEVERE, ex.getMessage(), ex);
                                    sender.sendMessage("§cSomething went wrong.");
                                }
                                return true;
                            case "remove":
                                if (args.length == 2) {
                                    sender.sendMessage("§cSpecify name of the service to remove. (cAsE sEnSiTiVe)");
                                    return true;
                                }

                                try {
                                    String name = args[2];

                                    PreparedStatement ps = this.connection.prepareStatement("DELETE FROM vote_services WHERE name = ?");
                                    ps.setString(1, name);
                                    if (ps.executeUpdate() > 0) {
                                        sender.sendMessage("§2Successfully removed service with name §n" + name + "§r§2.");
                                    } else {
                                        sender.sendMessage("§cNo service with name §n" + name + "§r§2.");
                                    }
                                } catch (SQLException ex) {
                                    getLogger().log(Level.SEVERE, ex.getMessage(), ex);
                                    sender.sendMessage("§cSomething went wrong.");
                                }
                                return true;
                            default:
                                sender.sendMessage("§cUnknown argument. Specify either list, set or remove as the second argument.");
                                return true;
                        }
                    case "fakevote":
                        if (args.length < 3) {
                            sender.sendMessage("§cSpecify player name and service name. (cAsE mAtTeRs)");
                            return true;
                        }

                        sender.sendMessage("§2Faking a vote...");
                        this.vote(args[1], args[2], "console");
                        return true;
                    default:
                        sender.sendMessage("§cUnknown argument. Use §4/votelistener help §cfor a list of commands.");
                        return true;
                }
        }
        return false;
    }

    @EventHandler
    public void onVotifierEvent(VotifierEvent event) {
        this.vote(
                event.getVote().getUsername(),
                event.getVote().getServiceName(),
                event.getVote().getAddress());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            PreparedStatement ps = this.connection.prepareStatement("SELECT * FROM votes WHERE player_id = (SELECT id FROM vote_players WHERE rewarded = false AND uuid = ?)",
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            ps.setString(1, event.getPlayer().getUniqueId().toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                this.reward(event.getPlayer());
                rs.updateBoolean("rewarded", true);
                rs.updateRow();
            }
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static String format(Player player, String message) {
        return message.replaceAll("%username%", player.getDisplayName());
    }

    private void reward(Player player) {
        player.sendMessage("§2You voted and will receive a reward!");

        for (String cmd : this.commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), format(player, cmd));
        }
    }

    /**
     * Register a vote for username.
     * @param username voter username.
     * @param serviceName name of service.
     * @param address ip-address of voter.
     */
    @SuppressWarnings("deprecation")
    private void vote(String username, String serviceName, String address) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
        String uuid = offlinePlayer.getUniqueId().toString();

        try {
            ResultSet service = this.getVoteService(serviceName);
            if (!service.next()) {
                getLogger().warning("A vote came in from a \"" + serviceName + "\" which is not added to the database! The vote therefore did not count. Make sure to add the service with the votelistener command.");
                return;
            }

            ResultSet player = this.getVotePlayer(uuid);
            if (!player.next()) {
                PreparedStatement ps = this.connection.prepareStatement("INSERT INTO vote_players (uuid) VALUES (?)");
                ps.setString(1, uuid);
                ps.executeUpdate();
                player = this.getVotePlayer(uuid);
                player.next();
            }

            ResultSet ip = this.getVoteIp(address);
            if (!ip.next()) {
                PreparedStatement ps = this.connection.prepareStatement("INSERT INTO vote_ips (address) VALUES (?)");
                ps.setString(1, address);
                ps.executeUpdate();
                ip = this.getVoteIp(address);
                ip.next();
            }

            Player onlinePlayer = offlinePlayer.getPlayer();
            boolean rewarded = false;
            if (onlinePlayer != null) {
                this.reward(onlinePlayer);
                rewarded = true;
            }

            PreparedStatement ps = this.connection.prepareStatement("INSERT INTO votes (player_id, service_id, ip_id, rewarded) VALUES (?, ?, ?, ?)");
            ps.setInt(1, player.getInt("id"));
            ps.setInt(2, service.getInt("id"));
            ps.setInt(3, ip.getInt("id"));
            ps.setBoolean(4, rewarded);
            ps.executeUpdate();
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }

        // TODO: broadcast message to server
        for (Player otherPlayer : getServer().getOnlinePlayers()) {
            if (!otherPlayer.getUniqueId().equals(offlinePlayer.getUniqueId())) {
                otherPlayer.sendMessage("§2A PLAYER VOTED AND GOT REWARDED!");
            }
        }
    }

    private ResultSet getVoteService(String name) throws SQLException {
        PreparedStatement ps = this.connection.prepareStatement("SELECT * FROM vote_services WHERE name = ?");
        ps.setString(1, name);
        return ps.executeQuery();
    }

    private ResultSet getVoteIp(String address) throws SQLException {
        PreparedStatement ps = this.connection.prepareStatement("SELECT * FROM vote_ips WHERE address = ?");
        ps.setString(1, address);
        return ps.executeQuery();
    }

    private ResultSet getVotePlayer(String uuid) throws SQLException {
        PreparedStatement ps = this.connection.prepareStatement("SELECT * FROM vote_players WHERE uuid = ?");
        ps.setString(1, uuid);
        return ps.executeQuery();
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

            // vote_players - id, uuid
            // vote_services - id, name, url, interval=24
            // vote_ips - id, address
            // votes - player_id, service_id, ip_id, timestamp

            Statement statement = this.connection.createStatement();
            statement.addBatch("CREATE SCHEMA IF NOT EXISTS " + schema + " AUTHORIZATION " + username);
            statement.addBatch("CREATE TABLE IF NOT EXISTS vote_players ( id SERIAL, uuid VARCHAR NOT NULL, PRIMARY KEY(id) )");
            statement.addBatch("CREATE TABLE IF NOT EXISTS vote_services ( id SERIAL, name VARCHAR UNIQUE NOT NULL, url VARCHAR NOT NULL, interval INTEGER DEFAULT 24, PRIMARY KEY(id) )");
            statement.addBatch("CREATE TABLE IF NOT EXISTS vote_ips ( id SERIAL, address VARCHAR NOT NULL, PRIMARY KEY(id) )");
            statement.addBatch("CREATE TABLE IF NOT EXISTS votes ( id SERIAL, player_id INTEGER REFERENCES vote_players(id) ON DELETE CASCADE, service_id INTEGER REFERENCES vote_services(id) ON DELETE CASCADE, ip_id INTEGER REFERENCES vote_ips(id) ON DELETE CASCADE, rewarded BOOL DEFAULT TRUE, timestamp TIMESTAMP DEFAULT NOW(), PRIMARY KEY(id) )");
            statement.executeBatch();

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

}
