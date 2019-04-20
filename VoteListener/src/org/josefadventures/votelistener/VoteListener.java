package org.josefadventures.votelistener;

import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

class RewardCommand {
    String command;
    boolean runAsConsole;
    boolean runAsOp;
    String require;
    int runAfterHours;
    String infoMessage;
}

public class VoteListener extends JavaPlugin implements Listener {

    // TODO: Permissions... right now everyone can set/delete services lmao
    // TODO: Make links into tellraw links, ie just anchors in chat instead of full url

    private Connection connection;

    private String conUrl = "jdbc:postgresql://host:port/database";
    private String conSchema = "votelistener";
    private String conUser = "";
    private String conPass = "";

    private String msgVoteReward = "§6You voted and got §c$25§6!";
    private String msgBroadcastVoted = "§1%player_name% §bvoted for the server and received §1$25§b!";
    private String msgVoteCmd = "§bClick on a link to vote and be rewarded with §1$25§b.";
    private String msgVoteCmdCanVote = "§aCan vote: §2%service_name% §a- §2%service_link%";
    private String msgVoteCmdCannotVote = "§cAlready voted: §4%service_name% §c- §4%service_link%";

    private ArrayList<RewardCommand> commands;
    private BukkitTask rewardTask;

    @Override
    public void onEnable() {
        getConfig().addDefault("msgBroadcastVoted", this.msgBroadcastVoted);
        getConfig().addDefault("msgVoteReward", this.msgVoteReward);
        getConfig().addDefault("msgVoteCmd", this.msgVoteCmd);
        getConfig().addDefault("msgVoteCmdCanVote", this.msgVoteCmdCanVote);
        getConfig().addDefault("msgVoteCmdCannotVote", this.msgVoteCmdCannotVote);

        if (!getConfig().isConfigurationSection("commands")) {
            getConfig().addDefault("commands.example.runAsConsole", true);
            getConfig().addDefault("commands.example.runAsOp", true);
            getConfig().addDefault("commands.example.command", "example %player_name% 2 Golden_Apple");
            getConfig().addDefault("commands.example.require", "votelistener.basicreward");
            getConfig().addDefault("commands.example.runAfterHours", 0);
            getConfig().addDefault("commands.example.infoMessage", "You have received 2 Gold Apples!");
        }

        getConfig().addDefault("postgresql_url", this.conUrl);
        getConfig().addDefault("postgresql_schema", this.conSchema);
        getConfig().addDefault("postgresql_username", this.conUser);
        getConfig().addDefault("postgresql_password", this.conPass);
        getConfig().options().copyDefaults(true);
        saveConfig();

        this.msgBroadcastVoted = getConfig().getString("msgBroadcastVoted");
        this.msgVoteReward = getConfig().getString("msgVoteReward");
        this.msgVoteCmd = getConfig().getString("msgVoteCmd");
        this.msgVoteCmdCanVote = getConfig().getString("msgVoteCmdCanVote");
        this.msgVoteCmdCannotVote = getConfig().getString("msgVoteCmdCannotVote");

        this.commands = new ArrayList<>();

        ConfigurationSection cs = getConfig().getConfigurationSection("commands");
        if (cs != null) {
            for (String key : cs.getKeys(false)) {
                ConfigurationSection cmdSection = cs.getConfigurationSection(key);
                if (cmdSection.isString("command")) {
                    this.commands.add(new RewardCommand() {{
                        this.command = cmdSection.getString("command");
                        this.runAsConsole = cmdSection.getBoolean("runAsConsole", true);
                        this.runAsOp = cmdSection.getBoolean("runAsOp", true);
                        this.require = cmdSection.getString("require", null);
                        this.runAfterHours = cmdSection.getInt("runAfterHours", 0);
                        this.infoMessage = cmdSection.getString("infoMessage", null);
                    }});
                }
            }
        }

        getLogger().info("Loaded " + this.commands.size() + " reward commands.");

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
            getLogger().info("Closed PostgreSQL connection.");
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

                sender.sendMessage(this.msgVoteCmd);
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
                            sender.sendMessage(format(rs.getString("name"), rs.getString("url"), this.msgVoteCmdCanVote));
                        } else {
                            sender.sendMessage(format(rs.getString("name"), rs.getString("url"), this.msgVoteCmdCannotVote));
                        }
                    }
                } catch (SQLException ex) {
                    getLogger().log(Level.SEVERE, ex.getMessage(), ex);
                    sender.sendMessage("§cSomething went wrong.");
                }
                return true;
            case "votelistener":
                if (!sender.hasPermission("votelistener.admin") && !sender.isOp()) {
                    sender.sendMessage("§cYou do not have access to this command.");
                    return true;
                }

                if (args.length == 0) {
                    sender.sendMessage("§cNot enough arguments. Use §n/votelistener help §r§cfor a list of commands.");
                    return true;
                }

                switch (args[0].toLowerCase()) {
                    case "help":
                        sender.sendMessage("List of commands:");
                        sender.sendMessage("/votelistener services list - List voting services.");
                        sender.sendMessage("/votelistener services add <service_name> <vote_url> [vote_interval_hours] [display_name] - Add voting service (service_name is CASE SENSITIVE!).");
                        sender.sendMessage("/votelistener services remove <service_name> - Remove voting service.");
                        sender.sendMessage("/votelistener fakevote <username> <service_name> - Cast a fake vote to test rewards.");
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
                this.reward(event.getPlayer(), rs.getInt("id"));
                rs.updateBoolean("rewarded", true);
                rs.updateRow();
            }

            ps = this.connection.prepareStatement("SELECT * FROM vote_commands WHERE vote_id IN (SELECT id FROM votes WHERE player_id = (SELECT id FROM vote_players WHERE uuid = ?))",
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            ps.setString(1, event.getPlayer().getUniqueId().toString());
            rs = ps.executeQuery();

            while (rs.next()) {
                String nCommand = rs.getString("command");
                String nInfoMessage = rs.getString("infoMessage");
                boolean nRunAsConsole = rs.getBoolean("runAsConsole");
                boolean nRunAsOp = rs.getBoolean("runAsOp");

                this.runRewardCommand(event.getPlayer(), new RewardCommand() {{
                    this.command = nCommand;
                    this.infoMessage = nInfoMessage;
                    this.runAsConsole = nRunAsConsole;
                    this.runAsOp = nRunAsOp;
                }});

                rs.deleteRow();
            }
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private static String format(Player player, String message) {
        return format(player.getDisplayName(), message);
    }

    private static String format(String displayName, String message) {
        return message.replaceAll("%player_name%", displayName);
    }

    private static String format(String serviceName, String serviceLink, String message) {
        return message.replaceAll("%service_name%", serviceName).replaceAll("%service_link%", serviceLink);
    }

    private void reward(Player player, int voteId) {
        player.sendMessage(format(player, this.msgVoteReward));

        for (RewardCommand cmd : this.commands) {
            if (cmd.require == null || player.hasPermission(cmd.require)) {
                if (cmd.runAfterHours == 0) {
                    this.runRewardCommand(player, cmd);
                } else {
                    this.runRewardCommandLater(player, cmd, voteId);
                }
            }
        }
    }

    private void runWaitingRewards() {
        try {
            PreparedStatement ps = this.connection.prepareStatement("SELECT DISTINCT ON (vote_commands.id) vote_commands.*, vote_players.uuid AS uuid " +
                    "FROM vote_commands " +
                    "LEFT JOIN vote_players ON vote_players.id = (SELECT player_id FROM votes WHERE votes.id = vote_commands.vote_id) " +
                    "WHERE runAt <= ?",
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            ps.setLong(1, System.currentTimeMillis());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Player player = Bukkit.getPlayer(UUID.fromString(rs.getString("uuid")));

                if (player == null) {
                    rs.updateBoolean("waitingForPlayerLogin", true);
                    rs.updateRow();
                } else {
                    RewardCommand rc = new RewardCommand() {{
                        this.command = rs.getString("command");
                        this.infoMessage = rs.getString("infoMessage");
                        this.runAsConsole = rs.getBoolean("runAsConsole");
                        this.runAsOp = rs.getBoolean("runAsOp");
                    }};

                    this.runRewardCommand(player, rc);
                    rs.deleteRow();
                }
            }
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }

        this.updateRewardSchedule();
    }

    /**
     * Update reward task schedule to run when the next reward command should be run.
     */
    private void updateRewardSchedule() {
        try {
            ResultSet rs = this.connection.prepareStatement("SELECT * FROM vote_commands WHERE waitingForPlayerLogin = false ORDER BY runAt ASC LIMIT 1")
                    .executeQuery();
            if (rs.next()) {
                long runAt = rs.getLong("runAt");
                long delta = runAt - System.currentTimeMillis();

                if (delta < 0) {
                    this.runWaitingRewards();
                    return;
                }

                if (this.rewardTask != null) {
                    this.rewardTask.cancel();
                }

                this.rewardTask = getServer().getScheduler().runTaskLater(this, this::runWaitingRewards, delta / 50 + 20); // 1 second extra wait for redundancy
            } else {
                if (this.rewardTask != null) {
                    this.rewardTask.cancel();
                    this.rewardTask = null;
                }
            }
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private void runRewardCommandLater(Player player, RewardCommand cmd, int voteId) {
        try {
            PreparedStatement ps = this.connection.prepareStatement(
                    "INSERT INTO vote_commands (vote_id, player_id, runAt, command, runAsConsole, runAsOp, infoMessage) " +
                    "VALUES (?, (SELECT player_id FROM votes WHERE id = ?), ?, ?, ?, ?, ?) ON CONFLICT (player_id, command) DO UPDATE SET runAt = EXCLUDED.runAt");
            ps.setInt(1, voteId);
            ps.setInt(2, voteId);
            ps.setLong(3, System.currentTimeMillis() + cmd.runAfterHours * 3600000);
            ps.setString(4, format(player, cmd.command));
            ps.setBoolean(5, cmd.runAsConsole);
            ps.setBoolean(6, cmd.runAsOp);
            ps.setString(7, cmd.infoMessage);
            ps.executeUpdate();
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }

        this.updateRewardSchedule();
    }

    private void runRewardCommand(Player player, RewardCommand cmd) {
        if (cmd.runAsConsole) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), format(player, cmd.command));
        } else {
            if (cmd.infoMessage != null) {
                player.sendMessage(cmd.infoMessage);
            }

            if (cmd.runAsOp) {
                boolean isOp = player.isOp();
                player.setOp(true);
                Bukkit.dispatchCommand(player, format(player, cmd.command));
                player.setOp(isOp);
            } else {
                Bukkit.dispatchCommand(player, format(player, cmd.command));
            }
        }

        if (cmd.infoMessage != null) {
            player.sendMessage(format(player, cmd.infoMessage));
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
                rewarded = true;
            }

            PreparedStatement ps = this.connection.prepareStatement("INSERT INTO votes (player_id, service_id, ip_id, rewarded) VALUES (?, ?, ?, ?) RETURNING id");
            ps.setInt(1, player.getInt("id"));
            ps.setInt(2, service.getInt("id"));
            ps.setInt(3, ip.getInt("id"));
            ps.setBoolean(4, rewarded);
            ResultSet rs = ps.executeQuery();
            rs.next();

            if (rewarded) {
                this.reward(onlinePlayer, rs.getInt("id"));
            }
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }

        for (Player otherPlayer : getServer().getOnlinePlayers()) {
            if (!otherPlayer.getUniqueId().equals(offlinePlayer.getUniqueId())) {
                otherPlayer.sendMessage(format(username, this.msgBroadcastVoted));
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
            // votes - id, player_id, service_id, ip_id, timestamp
            // vote_commands - vote_id, command, runAt (timestamp), waitingForPlayerLogin

            Statement statement = this.connection.createStatement();
            statement.addBatch("CREATE SCHEMA IF NOT EXISTS " + schema + " AUTHORIZATION " + username);
            statement.addBatch("CREATE TABLE IF NOT EXISTS vote_players ( id SERIAL, uuid VARCHAR NOT NULL, PRIMARY KEY(id) )");
            statement.addBatch("CREATE TABLE IF NOT EXISTS vote_services ( id SERIAL, name VARCHAR UNIQUE NOT NULL, url VARCHAR NOT NULL, interval INT DEFAULT 20, PRIMARY KEY(id) )");
            statement.addBatch("CREATE TABLE IF NOT EXISTS vote_ips ( id SERIAL, address VARCHAR NOT NULL, PRIMARY KEY(id) )");
            statement.addBatch("CREATE TABLE IF NOT EXISTS votes ( id SERIAL, player_id INT REFERENCES vote_players(id) ON DELETE CASCADE, service_id INT REFERENCES vote_services(id) ON DELETE CASCADE, ip_id INT REFERENCES vote_ips(id) ON DELETE CASCADE, rewarded BOOL DEFAULT TRUE, timestamp TIMESTAMP DEFAULT NOW(), PRIMARY KEY(id) )");
            statement.addBatch("CREATE TABLE IF NOT EXISTS vote_commands ( id SERIAL, vote_id INT REFERENCES votes(id) ON DELETE CASCADE, player_id INT, runAt BIGINT NOT NULL, command VARCHAR NOT NULL, runAsConsole BOOL DEFAULT TRUE, runAsOp BOOL DEFAULT TRUE, infoMessage VARCHAR, waitingForPlayerLogin BOOL DEFAULT FALSE, PRIMARY KEY(id), CONSTRAINT player_command UNIQUE (player_id, command) )");
            statement.executeBatch();

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

}
