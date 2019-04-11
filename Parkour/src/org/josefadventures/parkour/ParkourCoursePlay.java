package org.josefadventures.parkour;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class ParkourCoursePlay {

    private ParkourCourse parkourCourse;
    private Player player;

    private ArrayList<Block> checkpoints;

    private Location currentCheckpoint;

    private long startTime;
    private int checkpointCounter = 0;

    public ParkourCoursePlay(Player player, ParkourCourse parkourCourse) {
        this.player = player;
        this.parkourCourse = parkourCourse;

        this.currentCheckpoint = parkourCourse.getSpawn();
        this.checkpoints = new ArrayList<>();

        World world = parkourCourse.getWorld();
        for (Vector vector : parkourCourse.getCheckpoints()) {
            this.checkpoints.add(vector.toLocation(world).getBlock());
        }

        this.startTime = System.currentTimeMillis();

    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent ev) {
        if (ev.getAction() == Action.RIGHT_CLICK_BLOCK || ev.getAction() == Action.RIGHT_CLICK_AIR) {
            this.player.teleport(this.currentCheckpoint);
            this.checkpointCounter++;
        } else {
            this.player.sendMessage("Restarting!");
            this.checkpointCounter = 0;
            this.player.teleport(this.parkourCourse.getSpawn());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent ev) {
        if (ev.getTo().getBlock() != ev.getFrom().getBlock()) {
            if (this.checkpoints.contains(ev.getTo().getBlock())) {
                Bukkit.getScheduler().runTaskLater(null, () -> {
                    ev.getPlayer().sendMessage("New checkpoint!");
                    this.currentCheckpoint = ev.getPlayer().getLocation();
                }, 12);
            }
        }
    }

}
