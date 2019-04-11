package org.josefadventures.parkour;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.UUID;

public class ParkourCourse {

    private UUID uuid;

    public UUID getUniqueId() {
        return this.uuid;
    }

    private Vector positionSign;
    private Vector positionSpawn;
    private Vector positionGoal;
    private String nameCourse;
    private String nameCreator;
    private UUID uuidCreator;
    private UUID uuidWorld;

    private ArrayList<Vector> checkpoints;

    public ArrayList<Vector> getCheckpoints() {
        return this.checkpoints;
    }

    public String getName() {
        return this.nameCourse;
    }

    public int getNumCheckpoints() {
        return this.checkpoints.size();
    }

    public String getLocationString() {
        return this.uuidWorld + ":" + this.positionSign.getBlockX() + ":" + this.positionSign.getBlockY() + ":" + this.positionSign.getBlockZ();
    }

    public Location getSpawn() {
        return this.positionSpawn.toLocation(Bukkit.getWorld(this.uuidWorld));
    }

    public World getWorld() {
        return Bukkit.getWorld(this.uuidWorld);
    }

    public void setName(String name) {
        this.nameCourse = name;
    }

    public boolean setSpawn(Location location) {
        if (location.getWorld().getUID().equals(this.uuidWorld)) {
            this.positionSpawn = location.toVector();
            return true;
        }
        return false;
    }

    public boolean setGoal(Location location) {
        if (location.getWorld().getUID().equals(this.uuidWorld)) {
            this.positionGoal = location.toVector();
            return true;
        }
        return false;
    }

    public boolean isCreator(Player player) {
        return this.uuidCreator == player.getUniqueId();
    }

    public boolean isCheckpoint(Location location) {
        return this.checkpoints.contains(location.getBlock().getLocation().toVector());
    }

    public ParkourCourse(Player creator, Location signLocation) {
        this.uuid = UUID.randomUUID();

        this.nameCreator = creator.getDisplayName();
        this.uuidCreator = creator.getUniqueId();
        this.positionSign = signLocation.toVector();
        this.positionSpawn = signLocation.toVector();
        this.uuidWorld = signLocation.getWorld().getUID();

        this.checkpoints = new ArrayList<>();
    }

    public ParkourCourse(UUID uuid, String nameCourse, UUID uuidWorld, Vector positionSign, Vector positionSpawn, Vector positionGoal, String nameCreator, UUID uuidCreator) {
        this.uuid = uuid;
        this.nameCourse = nameCourse;
        this.uuidWorld = uuidWorld;
        this.positionSign = positionSign;
        this.positionSpawn = positionSpawn;
        this.positionGoal = positionGoal;
        this.nameCreator = nameCreator;
        this.uuidCreator = uuidCreator;
    }

    public boolean toggleCheckpointAt(Location location) {
        if (location.getWorld().getUID().equals(this.uuidWorld)) {
            Vector locVector = location.toVector();
            for (Vector vector : this.checkpoints) {
                if (vector.equals(locVector)) {
                    this.checkpoints.remove(locVector);
                    return false;
                }
            }
            this.checkpoints.add(locVector);
            return true;
        }
        return false;
    }



    @Override
    public String toString() {
        return ""
                + this.uuid + ';'
                + this.nameCourse + ';'
                + this.uuidWorld + ';'
                + this.positionSign.getBlockX() + ':' + this.positionSign.getBlockY() + ':' + this.positionSign.getBlockZ() + ';'
                + this.positionSpawn.getBlockX() + ':' + this.positionSpawn.getBlockY() + ':' + this.positionSpawn.getBlockZ() + ';'
                + this.positionGoal.getBlockX() + ':' + this.positionGoal.getBlockY() + ':' + this.positionGoal.getBlockZ() + ';'
                + this.nameCreator + ';'
                + this.uuidCreator;
    }

    public static ParkourCourse fromString(String string) {
        if (string == null) return null;

        String[] split = string.split(";");

        try {
            UUID uuid = UUID.fromString(split[0]);
            String nameCourse = split[1];
            UUID uuidWorld = UUID.fromString(split[2]);

            String[] posSignSplit = split[3].split(":");
            String[] posSpawnSplit = split[4].split(":");
            String[] posGoalSplit = split[5].split(":");

            Vector posSign = new Vector(Integer.parseInt(posSignSplit[0]), Integer.parseInt(posSignSplit[1]), Integer.parseInt(posSignSplit[2]));
            Vector posSpawn = new Vector(Integer.parseInt(posSpawnSplit[0]), Integer.parseInt(posSpawnSplit[1]), Integer.parseInt(posSpawnSplit[2]));
            Vector posGoal = new Vector(Integer.parseInt(posGoalSplit[0]), Integer.parseInt(posGoalSplit[1]), Integer.parseInt(posGoalSplit[2]));

            String nameCreator = split[6];
            UUID uuidCreator = UUID.fromString(split[7]);

            return new ParkourCourse(uuid, nameCourse, uuidWorld, posSign, posSpawn, posGoal, nameCreator, uuidCreator);
        } catch (Exception e) {
            return null;
        }
    }

}
