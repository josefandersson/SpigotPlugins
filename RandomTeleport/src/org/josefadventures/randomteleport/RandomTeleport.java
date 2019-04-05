package org.josefadventures.randomteleport;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.Teleport;
import com.earth2me.essentials.User;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Random;

public class RandomTeleport extends JavaPlugin implements Listener {

    private final String PERMISSION = "randomteleport.teleport";

    private final int MAX_TRIES = 50;

    private final int DEFAULT_MAX = 200;
    private final int DEFAULT_MIN = 50;
    private final String DEFAULT_GROUND = "both";
    private final String[] DISALLOWED_MATERIALS = new String[] {
            "LAVA",
            "WATER"
    };

    private Teleporter teleporter;

    @Override
    public void onEnable() {
        Essentials essentials = (Essentials) this.getServer().getPluginManager().getPlugin("Essentials");

        if (essentials == null) {
            System.out.println("Default teleporter");
            this.teleporter = new DefaultTeleporter();
        } else {
            System.out.println("Essentials teleporter");
            this.teleporter = new EssentialsTeleporter(essentials);
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION))
            return false;
        if (!(sender instanceof Player))
            return false;

        int min = DEFAULT_MIN;
        int max = DEFAULT_MAX;
        String relGr = DEFAULT_GROUND;

        switch (args.length) {
            case 3:
                relGr = args[2];
            case 2:
                min = Integer.parseInt(args[1]);
            case 1:
                max = Integer.parseInt(args[0]);
        }

        if (this.teleporter.teleportPlayer((Player) sender, min, max, relGr)) {
            sender.sendMessage("Swoosh...");
        } else {
            sender.sendMessage("Something went wrong");
        }

        return true;
    }

    // returns true if success
    private Location randomLocation(Player player, int min, int max, String relGround) {
        if (player == null)
            return null;

        boolean aboveGround;

        if (relGround.equalsIgnoreCase("true")) {
            aboveGround = true;
        } else if (relGround.equalsIgnoreCase("false")) {
            aboveGround = false;
        } else if (relGround.equalsIgnoreCase("both")) {
            aboveGround = new Random().nextBoolean();
        } else {
            return null;
        }

        for (int i = 0; i < MAX_TRIES; i++) {
            Location loc = this.randomLocation(player.getLocation().toVector(), min, max).toLocation(player.getWorld());
            loc.setY(255);

            if (aboveGround) {
                for (int y = 255; y > 1; y--) {
                    loc.subtract(0, 1, 0);
                    if (loc.getBlock().getType() != Material.AIR) {
                        if (this.isMaterialAllowed(loc.getBlock().getType())) {
                            return loc.add(0, 1, 0);
                        }
                        break;
                    }
                }
            } else {
                int numSolid = 0;
                int numAir = 0;
                for (int y = 255; y > 1; y--) {
                    loc.subtract(0, 1, 0);
                    if (numSolid < 4) {
                        if (loc.getBlock().getType() != Material.AIR) {
                            numSolid++;
                        }
                    } else {
                        if (loc.getBlock().getType() == Material.AIR) {
                            numAir++;
                        } else {
                            if (numAir >= 3) {
                                if (this.isMaterialAllowed(loc.getBlock().getType())) {
                                    return loc.add(0, 1, 0);
                                }
                            }
                            numAir = 0;
                        }
                    }
                }
            }
        }

        return null;
    }

    private Vector randomLocation(Vector origin, int min, int max) {
        int radius = new Random().nextInt(max - min) + min;
        double angle = new Random().nextDouble() * Math.PI * 2;

        double x = Math.cos(angle) * radius + origin.getX();
        double z = Math.sin(angle) * radius + origin.getZ();

        return new Vector(x, 256, z);
    }

    private boolean isMaterialAllowed(Material mat) {
        for (String name : DISALLOWED_MATERIALS) {
            if (mat.name().equals(name)) {
                return false;
            }
        }
        return true;
    }

    interface Teleporter {
        boolean teleportPlayer(Player player, int min, int max, String relGround);
    }

    class DefaultTeleporter implements Teleporter {
        @Override
        public boolean teleportPlayer(Player player, int min, int max, String relGround) {
            Location location = randomLocation(player, min, max, relGround);

            if (location != null) {
                player.teleport(location);
                return true;
            }

            return false;
        }
    }

    class EssentialsTeleporter implements Teleporter {
        private Essentials essentials;

        public EssentialsTeleporter(Essentials essentials) {
            this.essentials = essentials;
        }

        @Override
        public boolean teleportPlayer(Player player, int min, int max, String relGround) {
            Location location = randomLocation(player, min, max, relGround);

            if (location != null) {
                // TODO: Essentials teleportation not working? Not waiting for delays. There is however a delay afterwards for the next teleport.
                Teleport teleport = this.essentials.getUser(player).getTeleport();
                try {
                    teleport.cooldown(true);
                    teleport.teleport(location, null);
                    return true;
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
                    return false;
                }
            }

            return false;
        }
    }

}
