package org.josefadventures.seedplanter;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;

public class SeedPlanter extends JavaPlugin implements Listener {

    private final static String PERMISSION = "seedplanter.plant";

    private final static HashMap<Material, Material> PLANTABLES = new HashMap<Material, Material>() {
        {
            put(Material.WHEAT_SEEDS, Material.WHEAT);
            put(Material.BEETROOT_SEEDS, Material.BEETROOTS);
            put(Material.POTATO, Material.POTATOES);
            put(Material.CARROT, Material.CARROTS);
        }
    };

    private int maxSpread = 8;
    private int maxPlant = 64;
    private int tickDelay = 1;
    private int plantPerTick = 1;

    @Override
    public void onEnable() {
        this.getConfig().addDefault("maxSpread", this.maxSpread);
        this.getConfig().addDefault("maxPlant", this.maxPlant);
        this.getConfig().addDefault("tickDelay", this.tickDelay);
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();

        this.maxSpread = this.getConfig().getInt("maxSpread");
        this.maxPlant = this.getConfig().getInt("maxPlant");

        double tickDel = this.getConfig().getDouble("tickDelay");
        if (tickDel < 1) {
            this.tickDelay = 1;
            this.plantPerTick = (int) (1 / tickDel);
        } else {
            this.tickDelay = (int) tickDel;
        }

        this.getServer().getPluginManager().registerEvents(this, this);

        this.getLogger().info("Allowing spread of " + this.maxSpread + " blocks, maximum of " + this.maxPlant + " plants planted, with a delay of " + this.tickDelay + " and planting " + this.plantPerTick + " seeds every update.");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent ev) {
        if (ev.getPlayer().hasPermission(PERMISSION)) {
            if (ev.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (ev.getItem() != null) {
                    Material material = ev.getItem().getType();
                    for (Material key : PLANTABLES.keySet()) {
                        if (material == key) {
                            if (ev.getClickedBlock().getType() == PLANTABLES.get(key)) {
                                Planter planter = new Planter(ev.getClickedBlock(), ev.getPlayer(), ev.getHand());
                                BukkitTask task = this.getServer().getScheduler().runTaskTimer(this, () -> planter.onTick(), 0, tickDelay);
                                planter.setTask(task);
                            }
                        }
                    }
                }
            }
        }
    }

    class Planter {
        private Block source;
        private Material sourceMaterial;
        private Material seedMaterial;
        private Player player;
        private EquipmentSlot slot;

        private BukkitTask task;
        private boolean shouldCancel = false;

        private int planted = 0;
        private ArrayList<Integer> been;
        private ArrayList<Integer> disallowed; // when player can't place block there due to other plugins cancelling event

        Planter(Block source, Player player, EquipmentSlot slot) {
            this.source = source;
            this.player = player;
            this.slot = slot;
            this.sourceMaterial = this.source.getType();

            if (slot == EquipmentSlot.HAND) {
                this.seedMaterial = player.getInventory().getItemInMainHand().getType();
            } else if (slot == EquipmentSlot.OFF_HAND) {
                this.seedMaterial = player.getInventory().getItemInOffHand().getType();
            }

            this.been = new ArrayList<>();
            this.disallowed = new ArrayList<>();
        }

        public void cancel() {
            this.shouldCancel = true;
            if (this.task != null) {
                this.task.cancel();
            }
        }

        public void setTask(BukkitTask task) {
            this.task = task;
        }

        private ItemStack getItemStackInUsedHand() {
            ItemStack itemStack = null;

            if (this.slot == EquipmentSlot.HAND) {
                itemStack = this.player.getInventory().getItemInMainHand();
            } else if (this.slot == EquipmentSlot.OFF_HAND) {
                itemStack = this.player.getInventory().getItemInOffHand();
            }

            if (itemStack != null && itemStack.getType().equals(this.seedMaterial)) {
                return itemStack;
            } else {
                return null;
            }
        }

        public void onTick() {
            if (this.shouldCancel) {
                this.cancel();
                return;
            }

            for (int i = 0; i < plantPerTick; i++) {
                if (this.plantNext()) {
                    if (this.planted >= maxPlant) {
                        this.cancel();
                        return;
                    }
                } else {
                    this.cancel();
                    return;
                }
            }
        }

        private boolean plantNext() {
            ItemStack itemStack = this.getItemStackInUsedHand();
            if (itemStack != null && itemStack.getAmount() > 0) {
                this.been.clear();
                Block nextBlock = this.floodFind(0, 0, 0, 0);
                if (nextBlock != null) {
                    BlockState oldState = nextBlock.getState();
                    nextBlock.setType(this.sourceMaterial);

                    BlockPlaceEvent event = new BlockPlaceEvent(oldState.getBlock(), oldState, nextBlock, itemStack, this.player, true, this.slot);
                    getServer().getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        oldState.update(true); // revert to old block
                        this.disallowed.add(nextBlock.getX() * (maxSpread + 1) * 2 + nextBlock.getZ());
                        return false;
                    }

                    itemStack.setAmount(itemStack.getAmount() - 1);
                    this.planted++;

                    return true;
                }
            }
            return false;
        }

        private Block floodFind(int x, int z, int fromX, int fromZ) {
            if (x <= -maxSpread || x >= maxSpread
                    || z <= -maxSpread || z >= maxSpread)
                return null;

            int hereId = x * (maxSpread + 1) * 2 + z;
            if (this.been.contains(hereId)
                    || this.disallowed.contains(hereId))
                return  null;
            else
                this.been.add(hereId);

            Block here = this.source.getRelative(x, 0, z);

            if (here.getType() == Material.AIR) {
                Block base = here.getRelative(BlockFace.DOWN);
                if (base.getType() == Material.FARMLAND) {
                    return here;
                } else {
                    return null;
                }
            }

            if (here.getType() != this.sourceMaterial)
                return null;

            Block found;

            if (!(fromX == x && fromZ == z - 1)) {
                found = this.floodFind(x, z - 1, x, z);
                if (found != null)
                    return found;
            }

            if (!(fromX == x && fromZ == z + 1)) {
                found = this.floodFind(x, z + 1, x, z);
                if (found != null)
                    return found;
            }

            if (!(fromX == x - 1 && fromZ == z)) {
                found = this.floodFind(x - 1, z, x, z);
                if (found != null)
                    return found;
            }

            if (!(fromX == x + 1 && fromZ == z)) {
                found = this.floodFind(x + 1, z, x, z);
                if (found != null)
                    return found;
            }

            return null;
        }

    }

}
