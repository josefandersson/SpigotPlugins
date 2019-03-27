package org.josefadventures.miningworld;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Logger;

public class MiningWorld extends JavaPlugin implements Listener {

    private final String PERMISSION = "miningworld.trusted";

    private Logger logger;

    private FileConfiguration config;

    private String worldName = "mining";
    private String message = "Surface cannot be altered! Find a cave to mine in instead!";
    private int messageTime = 10000;
    private int numPropagations = 7;
    private int minY = 45;
    private int freeHeight = 20;

    private ArrayList<Material> disallowedMaterials;
    private HashMap<UUID, Long> messageTimeout;

    @Override
    public void onEnable() {

        this.config = this.getConfig();
        this.logger = this.getLogger();

        this.config.addDefault("miningWorldName", this.worldName);
        this.config.addDefault("warningMessage", this.message);
        this.config.addDefault("timeBetweenWarnings", this.messageTime);
        this.config.addDefault("blocksFromSunlight", this.numPropagations);
        this.config.addDefault("minimumYToCheck", this.minY);
        this.config.addDefault("checkHeightAbove", this.freeHeight);
        this.config.addDefault("disallowedMaterials", new String[] {
                Material.GRASS_BLOCK.name(),
                Material.DIRT.name(),
                Material.STONE.name(),
                Material.COBBLESTONE.name(),
                Material.SAND.name(),
                Material.ANDESITE.name(),
                Material.DIORITE.name(),
                Material.GRANITE.name(),
                Material.SANDSTONE.name()
        });
        this.config.options().copyDefaults(true);
        this.saveConfig();

        this.worldName = this.config.getString("miningWorldName");
        this.message = this.config.getString("warningMessage");
        this.messageTime = this.config.getInt("timeBetweenWarnings");
        this.numPropagations = this.config.getInt("blocksFromSunlight");
        this.minY = this.config.getInt("minimumYToCheck");
        this.freeHeight = this.config.getInt("checkHeightAbove");
        this.disallowedMaterials = this.stringListToMaterialList(this.config.getStringList("disallowedMaterials"));

        this.messageTimeout = new HashMap<>();

        this.getServer().getPluginManager().registerEvents(this, this);

        this.logger.info("Enabled without problems");
    }

    @Override
    public void onDisable(){
        this.messageTimeout.clear();

        this.logger.info("Disabled without problems");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent ev) {
        this.messageTimeout.remove(ev.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onBlockCanBuildEvent(BlockCanBuildEvent ev) {
        if (!this.isInMiningWorld(ev.getPlayer().getWorld())) return;
        if (this.playerHasPermission(ev.getPlayer())) return;
        if (this.isMaterialAllowed(ev.getMaterial())) return;

        if (!this.checkLocation(ev.getBlock().getLocation())) {
            ev.setBuildable(false);
            warnPlayer(ev.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent ev) {
        if (ev.isCancelled()) return;
        if (!this.isInMiningWorld(ev.getPlayer().getWorld())) return;
        if (this.playerHasPermission(ev.getPlayer())) return;
        if (ev.getClickedBlock() == null) return;

        if (ev.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (this.isMaterialAllowed(ev.getClickedBlock().getType())) return;
            if (!this.checkLocation(ev.getClickedBlock().getLocation())) {
                ev.setCancelled(true);
                warnPlayer(ev.getPlayer());
            }
        }
        /*else if (ev.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!this.checkLocation(ev.getClickedBlock().getLocation())) {
                ev.setCancelled(true);
                warnPlayer(ev.getPlayer());
            }
        }*/
    }

    private void warnPlayer(Player player) {
        if (this.messageTimeout.containsKey(player.getUniqueId())) {
            if (this.messageTimeout.get(player.getUniqueId()) < System.currentTimeMillis()) {
                this.messageTimeout.replace(player.getUniqueId(), System.currentTimeMillis() + this.messageTime);
            } else {
                return;
            }
        } else {
            this.messageTimeout.put(player.getUniqueId(), System.currentTimeMillis() + this.messageTime);
        }

        player.sendMessage(this.message);
    }

    private boolean playerHasPermission(Player player) {
        return player.hasPermission(PERMISSION);
    }

    private boolean isMaterialAllowed(Material material) {
        boolean isAllowed = true;
        for (Material mat : this.disallowedMaterials) {
            if (mat.equals(material)) {
                isAllowed = false;
                break;
            }
        }
        return isAllowed;
    }

    private boolean isInMiningWorld(World world) {
        if (world == null) return false;

        return world.getName().equals(this.worldName);
    }

    // return true if location is okay to be mined
    private boolean checkLocation(Location location) {
        if (location == null) return true;
        if (location.getY() <= this.minY) return true;

        return !this.propagate(location, this.numPropagations, -1);
    }

    private Vector[] directions = new Vector[] {
            new Vector(1, 0, 0),
            new Vector(0, 1, 0),
            new Vector(0, 0, 1),
            new Vector(-1, 0, 0),
            new Vector(0, -1, 0),
            new Vector(0, 0, -1)
    };

    // returns true if block within propagation only has air above
    private boolean propagate(Location location, int propagations, int ignoreDirection) {
        if (propagations <= 0) return false;

        boolean onlyAir = true;

        for (int y = 1; y < this.freeHeight + 1; y++) {
            if (location.add(0, 1, 0).getBlock().getType() != Material.AIR) {
                onlyAir = false;
                break;
            }
        }

        if (onlyAir) {
            return true;
        }

        int nextPropagations;

        Location nextLocation;
        Material nextMaterial;
        for (int i = 0; i < 6; i++) {
            if (i == ignoreDirection) continue;

            nextPropagations = propagations - 1;
            nextLocation = location.clone().add(this.directions[i]);
            nextMaterial = nextLocation.getBlock().getType();
            if (nextMaterial != Material.AIR) {
                nextPropagations -= 2;
            }

            if (this.propagate(nextLocation, nextPropagations, (i + 3) % 6)) {
                return true;
            }
        }

        return false;
    }

    private ArrayList<Material> stringListToMaterialList(List<String> strings) {
        ArrayList<Material> materials = new ArrayList<>();
        for (String name : strings) {
            materials.add(Material.getMaterial(name));
        }
        return materials;
    }

}
