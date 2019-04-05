package org.josefadventures.votelistener;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerData {

    private File file;
    private FileConfiguration fileConfiguration;

    private String username;
    private UUID uuid;

    private ArrayList<VoteData> votes = new ArrayList<>();
    private int totalVotes = 0;
    private long lastVote = 0;

    private PlayerData(String username, UUID uuid) {
        this.username = username;
        this.uuid = uuid;

        this.file = new File(VoteListener.instance.getDataFolder(), this.uuid.toString() + ".yml");
        this.fileConfiguration = YamlConfiguration.loadConfiguration(this.file);

        this.load();
    }

    public PlayerData(Player player) {
        this(player.getName(), player.getUniqueId());
    }

    public PlayerData(OfflinePlayer offlinePlayer) {
        this(offlinePlayer.getName(), offlinePlayer.getUniqueId());
    }

    // check if player has any pending votes and then rewards or removes accordingly
    public void check(Player player) {
        if (this.votes.size() > 0) {
            for (VoteData voteData : this.votes) {

            }
        }
    }

    public void load() {
        try {
            this.fileConfiguration.load(this.file);
        } catch (InvalidConfigurationException | IOException ex) {
            VoteListener.instance.getLogger().warning("Couldn't load player data for " + this.username + ": " + ex.getMessage());
            return;
        }

        this.votes.clear();

        this.totalVotes = this.fileConfiguration.getInt("totalVotes", this.totalVotes);
        this.lastVote = this.fileConfiguration.getLong("lastVote", this.lastVote);

        List<String> rawVotes = this.fileConfiguration.getStringList("votes");
        for (String rawVote : rawVotes) {
            VoteData voteData = VoteData.fromString(rawVote);
            if (voteData == null) {
                VoteListener.instance.getLogger().warning("Tried to load invalid vote data for " + this.username + ".");
            } else {
                this.votes.add(voteData);
            }
        }
    }

    public void save() {
        ArrayList<String> rawVotes = new ArrayList<>();
        for (VoteData voteData : this.votes) {
            rawVotes.add(voteData.toString());
        }

        this.fileConfiguration.set("totalVotes", this.totalVotes);
        this.fileConfiguration.set("lastVote", this.lastVote);
        this.fileConfiguration.set("votes", rawVotes);

        try {
            this.fileConfiguration.save(this.file);
        } catch (IOException ex) {
            VoteListener.instance.getLogger().warning("Couldn't save player data for " + this.username + ": " + ex.getMessage());
        }
    }

}
