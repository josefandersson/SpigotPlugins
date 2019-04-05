package org.josefadventures.votelistener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class VoteData {

    private String service;
    private long timestamp;
    private String[] commandsStart; // cmds to run when this player logs on
    private String[] commandsEnd;
    private long period; // delay in millis between start and end commands
    private boolean beenRewarded; // if player already has been rewarded for this vote

    public VoteData(String service, long timestamp, String[] commandsStart, String[] commandsEnd, long period) {
        this.service = service;
        this.timestamp = timestamp;
        this.commandsStart = commandsStart;
        this.commandsEnd = commandsEnd;
        this.period = period;
    }

    public void check(Player player) {
        if (!this.beenRewarded) {
            this.runStartCommands(player);
            if (this.commandsEnd.length == 0) {

            }
            // if no end commands, remove this VoteData
            this.beenRewarded = true;
        }

        if (this.timestamp + this.period < System.currentTimeMillis()) {
            // run end commands
            // remove this VoteData
            return;
        }

        // setup bukkit scheduler to run end commands
    }

    private void runStartCommands(Player player) {
        for (String cmd : this.commandsStart) {
            if (!Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd)) {
                VoteListener.instance.getLogger().warning("Could not execute command: " + cmd);
            }
        }

        player.sendMessage(ChatColor.GOLD + "You got rewarded for voting!");
    }

    private void runEndCommands(Player player) {
        for (String cmd : this.commandsEnd) {
            if (!Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd)) {
                VoteListener.instance.getLogger().warning("Could not execute command: " + cmd);
            }
        }

        player.sendMessage(ChatColor.GOLD + "You lost your privileges, vote again to regain them!");
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("v1");

        builder.append(";;");
        builder.append(this.service);

        builder.append(";;");
        builder.append(this.timestamp);

        builder.append(";;");
        for (String cmd : this.commandsStart) {
            builder.append(cmd);
            builder.append(';');
        }

        builder.append(";;");
        for (String cmd : this.commandsStart) {
            builder.append(cmd);
            builder.append(';');
        }

        return builder.toString();
    }

    public static VoteData fromString(String string) {
        if (string == null || string.length() == 0) return null;

        String[] parts = string.split(";;");

        if (parts.length != 6) return null;

        try {
            return new VoteData(
                    parts[1],
                    Long.parseLong(parts[2]),
                    parts[3].split(";"),
                    parts[4].split(";"),
                    Long.parseLong(parts[5]));
        } catch (Exception ex) {
            return null;
        }
    }

}
