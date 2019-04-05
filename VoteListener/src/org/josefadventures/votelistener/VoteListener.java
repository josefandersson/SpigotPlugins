package org.josefadventures.votelistener;

import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class VoteListener extends JavaPlugin implements Listener {

    public static VoteListener instance;

    public String broadcastVotedMessage = "§6Someone voted for the server and got §c$25§6! Use §c/vote §6and do the same.";
    public String playerRewardMessage = "§6You voted and got §c$25§6!";
    public String playerLostRewardMessage;
    public String playerNotifyMessage;

    public List<String> commands;

    private HashMap<UUID, Integer> voted;

    @Override
    public void onEnable() {
        instance = this;

        this.getConfig().addDefault("broadcastVotedMessage", this.broadcastVotedMessage);
        this.getConfig().addDefault("playerRewardMessage", this.playerRewardMessage);
        this.getConfig().addDefault("playerLostRewardMessage", this.playerLostRewardMessage);
        this.getConfig().addDefault("playerNotifyMessage", this.playerNotifyMessage);
        this.getConfig().addDefault("commands", new String[] {});
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();

        this.broadcastVotedMessage = this.getConfig().getString("broadcastVotedMessage");
        this.playerRewardMessage = this.getConfig().getString("playerRewardMessage");
        this.playerLostRewardMessage = this.getConfig().getString("playerLostRewardMessage");
        this.playerNotifyMessage = this.getConfig().getString("playerNotifyMessage");
        this.commands = this.getConfig().getStringList("commands");

        this.voted = new HashMap<>();

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onVotifierEvent(VotifierEvent event) {
        Player player = Bukkit.getPlayer(event.getVote().getUsername());

        for (Player p : this.getServer().getOnlinePlayers()) {
            if (p != player) {
                p.sendMessage(format(p, this.broadcastVotedMessage));
            }
        }

        if (player == null) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(event.getVote().getUsername());

            UUID uuid = offlinePlayer.getUniqueId();
            int missedVotes = this.voted.getOrDefault(uuid, 0) + 1;

            this.voted.put(uuid, missedVotes);
        } else {
            player.sendMessage(format(player, this.playerRewardMessage));
            for (String cmd : this.commands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), format(player, cmd));
            }
        }

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        if (this.voted.containsKey(uuid)) {
            for (int i = this.voted.get(uuid); 0 < i; i--) {
                event.getPlayer().sendMessage(format(event.getPlayer(), this.playerRewardMessage));
                for (String cmd : this.commands) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), format(event.getPlayer(), cmd));
                }
            }
            this.voted.remove(uuid);
        }
    }

    public static String format(Player player, String message) {
        return message.replaceAll("%username%", player.getDisplayName());
    }


}
