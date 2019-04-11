package org.josefadventures.parkour;

import com.mojang.datafixers.types.Func;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class Parkour extends JavaPlugin implements Listener {

    /*
    * - Place sign with [Parkour] will create a new parkour course.
    *   : Sign will turn red when the course is unfinished
    *   : Green when course if finished and can be played
    * - Right click sign will:
    *   : If unfinished:
    *       - Put owner into edit mode, giving him the parkour editing tool
    *   : If finished:
    *       - Put player into parkour mode, giving him the parkour playing tool
    * - Parkour editing tool:
    *   : Named "Parkour Editing Tool" in blue
    *   : Description: "Left-click to add or remove current position as a checkpoint.
    *                   Right-click to open tools menu.
    *                   Course has
    * - Parkour playing tool:
    *   : Named "Parkour Controller" in blue
    *   : Description: "Left-click to restart course.
    *                   Right-click to go to last checkpoint.
    *                   You are playing <name-of-course> [by <creator>].
    *                   Best time for this course is <best-time> [by <best-timed-player>]."
    *   : Left-click:
    *       - Prompt to restart the map
    *   : Right-click:
    *       - Go to last checkpoint
    *  - Editing menu:
    *   : One row, 9 slots
    *   : [S,G, ,N, , ,D, ,X]:
    *       - S: set spawn position to player position (default is the block in front of the sign)
    *       - G: set goal position to player position
    *       - N: change name of course (inventory will close and player will be prompted for a new name in chat)
    *       - D: delete course (will prompt in chat to type YES before deleting)
    *       - X: close button
    *
    * */

    private Material materialEditTool;
    private String nameEditTool;
    private List<String> loreEditTool;
    private String titleEditMenu;

    private Material materialPlayingTool;
    private String namePlayingTool;
    private List<String> lorePlayingTool;

    private String nameButtonClose;
    private String nameButtonDelete;
    private String nameButtonSpawn;
    private String nameButtonGoal;
    private String nameButtonName;

    private HashMap<String, ParkourCourse> parkourCourses;
    private HashMap<UUID, ParkourCoursePlay> playingCourse;
    private HashMap<UUID, ParkourCourse> editingCourse;
    private HashMap<UUID, ParkourCourse> openMenus;
    private HashMap<UUID, Consumer<String>> waitingForChat;

    @Override
    public void onEnable() {
        this.materialEditTool = Material.IRON_NUGGET;
        this.materialPlayingTool = Material.GOLD_NUGGET;
        this.nameEditTool = ChatColor.BLUE + "Parkour Editing Tool";
        this.namePlayingTool = ChatColor.BLUE + "Parkour Controller";
        this.loreEditTool = Arrays.asList("");
        this.lorePlayingTool = Arrays.asList("Left-click to restart course.", "Right-click to go to last checkpoint.", "You are playing %COURSE_NAME% by %COURSE_CREATOR%.", "Best time for this course is %BEST_TIME% by %BEST_PLAYER%.");
        this.titleEditMenu = "Editing Menu";

        this.nameButtonClose = "Close Menu";
        this.nameButtonDelete = "Delete Parkour Course";
        this.nameButtonSpawn = "Set Spawn Position";
        this.nameButtonGoal = "Set Goal Position";
        this.nameButtonName = "Change Parkour Couse Name";

        this.parkourCourses = new HashMap<>();
        this.openMenus = new HashMap<>();
        this.waitingForChat = new HashMap<>();

        this.playingCourse = new HashMap<>();
        this.editingCourse = new HashMap<>();

        this.getServer().getPluginManager().registerEvents(this, this);

        // TODO: load courses from file
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSignChange(SignChangeEvent ev) {
        System.out.println(ev.getLine(0));
        if (ev.getLine(0).equalsIgnoreCase("[parkour]")) {
            ParkourCourse parkourCourse = new ParkourCourse(ev.getPlayer(), ev.getBlock().getLocation());

            this.parkourCourses.put(parkourCourse.getLocationString(), parkourCourse);
            ev.getPlayer().sendMessage("Created parkour course. Right-click sign two times to enter edit mode.");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent ev) {
        if (!this.playingCourse.isEmpty() && this.playingCourse.containsKey(ev.getPlayer().getUniqueId())) {
            this.playingCourse.get(ev.getPlayer().getUniqueId()).onPlayerInteract(ev);
        } else if (!this.editingCourse.isEmpty() && this.editingCourse.containsKey(ev.getPlayer().getUniqueId())) {
            ParkourCourse parkourCourse = this.editingCourse.get(ev.getPlayer().getUniqueId());
            if (ev.getAction() == Action.RIGHT_CLICK_AIR || ev.getAction() == Action.RIGHT_CLICK_BLOCK) {
                this.openEditMenu(ev.getPlayer(), parkourCourse);
            } else {
                if (parkourCourse.toggleCheckpointAt(ev.getPlayer().getLocation())) {
                    ev.getPlayer().sendMessage("Your current location is now a checkpoint. (Total: " + parkourCourse.getNumCheckpoints() + ")");
                } else {
                    ev.getPlayer().sendMessage("Your current location is no longer a checkpoint. (Total: " + parkourCourse.getNumCheckpoints() + ")");
                }
            }
        }

        if (ev.getAction() == Action.RIGHT_CLICK_BLOCK) {
            BlockData blockData = ev.getClickedBlock().getBlockData();
            if (blockData instanceof Sign) {
                ParkourCourse parkourCourse = this.parkourCourses.get(locationToString(ev.getClickedBlock().getLocation()));
                if (parkourCourse != null) {
                    if (this.playingCourse.containsKey(ev.getPlayer().getUniqueId())) {
                        this.playingCourse.remove(ev.getPlayer().getUniqueId());
                        if (parkourCourse.isCreator(ev.getPlayer())) { // TODO: Or has permission to edit other's
                            if (this.editingCourse.containsKey(ev.getPlayer().getUniqueId())) {
                                this.leaveCourse(ev.getPlayer());
                                ev.getPlayer().sendMessage("You left editing mode for the parkour course.");
                            } else {
                                this.editCourse(ev.getPlayer(), parkourCourse);
                                ev.getPlayer().sendMessage("You entered editing mode for the parkour course.");
                            }
                        } else {
                            this.leaveCourse(ev.getPlayer());
                            ev.getPlayer().sendMessage("You left the parkour course.");
                        }
                    } else {
                        this.playCourse(ev.getPlayer(), parkourCourse);
                        ev.getPlayer().sendMessage("You entered the parkour course.");
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent ev) {
        if (!this.openMenus.isEmpty()) {
            if (this.openMenus.containsKey(ev.getWhoClicked().getUniqueId())) {
                if (ev.getInventory().getTitle().equals(this.titleEditMenu)) {
                    ev.setCancelled(true);
                    ParkourCourse parkourCourse = this.openMenus.get(ev.getWhoClicked().getUniqueId());
                    Player player = (Player) ev.getWhoClicked();
                    switch (ev.getRawSlot()) {
                        case 0:
                            if (parkourCourse.setSpawn(player.getLocation())) {
                                player.sendMessage("Updated spawn position for parkour course.");
                            } else {
                                player.sendMessage("Could not update spawn position for parkour course. Are you in the same World?");
                            }
                            break;
                        case 1:
                            if (parkourCourse.setGoal(player.getLocation())) {
                                player.sendMessage("Updated goal position for parkour course.");
                            } else {
                                player.sendMessage("Could not update goal position for parkour course. Are you in the same World?");
                            }
                            break;
                        case 3:
                            player.closeInventory();
                            this.openMenus.remove(player.getUniqueId());

                            player.sendMessage("You are about to change the name of your parkour course.");
                            player.sendMessage("The name has to be at least 3 characters long and maximum 14 characters long.");
                            player.sendMessage("Type a new name in the chat within 30 seconds. Type CANCEL to cancel.");

                            this.waitingForChat.put(player.getUniqueId(), message -> {
                                if (message.equalsIgnoreCase("cancel")) {
                                    this.waitingForChat.remove(player.getUniqueId());
                                    player.sendMessage("Your name change request was cancelled.");
                                    openEditMenu(player, parkourCourse);
                                } else {
                                    String name = message.trim();
                                    if (name.length() >= 3 && name.length() <= 14) {
                                        // TODO: check if course name already exists
                                        parkourCourse.setName(name);
                                        player.sendMessage("Updated name for parkour course.");
                                        this.waitingForChat.remove(player.getUniqueId());
                                    } else {
                                        // TODO: reset 30 second timeout
                                        player.sendMessage("The name has to be at least 3 characters long and maximum 14 characters long.");
                                    }
                                }
                            });

                            getServer().getScheduler().runTaskLater(this, () -> {
                                if (this.waitingForChat.containsKey(ev.getWhoClicked().getUniqueId())) {
                                    this.waitingForChat.remove(ev.getWhoClicked().getUniqueId());
                                    ev.getWhoClicked().sendMessage("Your name change request timed out after 30 seconds.");
                                }
                            }, 30 * 20);
                            break;
                        case 6:
                            player.closeInventory();
                            this.openMenus.remove(player.getUniqueId());

                            player.sendMessage("You are about to delete your parkour course.");
                            player.sendMessage("Type YES in the chat within 30 seconds to confirm. Type NO to cancel.");

                            this.waitingForChat.put(player.getUniqueId(), message -> {
                                this.waitingForChat.remove(player.getUniqueId());
                                if (message.equalsIgnoreCase("yes")) {
                                    player.sendMessage("Your parkour course is being deleted.");
                                    // parkourCourse.delete();
                                } else {
                                    player.sendMessage("Your delete request was cancelled.");
                                    openEditMenu(player, parkourCourse);
                                }
                            });

                            getServer().getScheduler().runTaskLater(this, () -> {
                                if (this.waitingForChat.containsKey(ev.getWhoClicked().getUniqueId())) {
                                    this.waitingForChat.remove(ev.getWhoClicked().getUniqueId());
                                    ev.getWhoClicked().sendMessage("Your delete request timed out after 30 seconds.");
                                }
                            }, 30 * 20);
                            break;
                        case 8:
                            player.closeInventory();
                            this.openMenus.remove(player.getUniqueId());
                            break;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent ev) {
        if (!this.openMenus.isEmpty()) {
            if (ev.getInventory().getTitle().equals(this.titleEditMenu)) {
                if (this.openMenus.containsKey(ev.getPlayer().getUniqueId())) {
                    this.openMenus.remove(ev.getPlayer().getUniqueId());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent ev) {
        this.leaveCourse(ev.getPlayer());
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent ev) {
        if (!this.playingCourse.isEmpty() && this.playingCourse.containsKey(ev.getPlayer().getUniqueId())) {
            if (ev.getItemDrop().getItemStack().getItemMeta().getDisplayName().equals(this.namePlayingTool)) {
                this.leaveCourse(ev.getPlayer());
            }
        } else if (!this.editingCourse.isEmpty() && this.editingCourse.containsKey(ev.getPlayer().getUniqueId())) {
            if (ev.getItemDrop().getItemStack().getItemMeta().getDisplayName().equals(this.nameEditTool)) {
                this.leaveCourse(ev.getPlayer());
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent ev) {
        if (!this.playingCourse.isEmpty() && this.playingCourse.containsKey(ev.getPlayer().getUniqueId())) {
            this.playingCourse.get(ev.getPlayer().getUniqueId()).onPlayerMove(ev);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldChange(PlayerChangedWorldEvent ev) {
        this.leaveCourse(ev.getPlayer());
    }

    public void removeTools(Player player) {
        for (ItemStack itemStack : player.getInventory())
            if (itemStack != null && itemStack.getItemMeta() != null)
                if (itemStack.getItemMeta().getDisplayName().equals(this.nameEditTool) || itemStack.getItemMeta().getDisplayName().equals(this.namePlayingTool))
                    player.getInventory().remove(itemStack);
    }

    public void leaveCourse(Player player) {
        this.playingCourse.remove(player.getUniqueId());
        this.editingCourse.remove(player.getUniqueId());
        this.removeTools(player);
    }

    public void editCourse(Player player, ParkourCourse parkourCourse) {
        if (player.getItemOnCursor().getAmount() != 0) {
            player.sendMessage("Clear your main hand of any items before editing the parkour course.");
        } else {
            player.setItemOnCursor(this.createEditTool());
            this.editingCourse.put(player.getUniqueId(), parkourCourse);
            player.sendMessage("You are now editing the parkour course \"" + parkourCourse.getName() + "\". To stop editing, drop the " + this.nameEditTool + ".");
        }
    }

    public void playCourse(Player player, ParkourCourse parkourCourse) {
        if (player.getItemOnCursor().getAmount() != 0) {
            player.sendMessage("Clear your main hand of any items before starting the parkour course.");
        } else {
            player.setItemOnCursor(this.createPlayingTool());
            this.playingCourse.put(player.getUniqueId(), new ParkourCoursePlay(player, parkourCourse));
            player.teleport(parkourCourse.getSpawn());
            player.sendMessage("You are now playing the parkour course \"" + parkourCourse.getName() + "\". To stop playing, drop the " + this.namePlayingTool + ".");
        }
    }

    public void openEditMenu(Player player, ParkourCourse parkourCourse) {
        Inventory inventory = Bukkit.createInventory(null, 9, this.titleEditMenu);
        inventory.setItem(0, this.createItemStack(Material.DARK_OAK_DOOR, 1, false, this.nameButtonSpawn, null));
        inventory.setItem(1, this.createItemStack(Material.IRON_DOOR, 1, false, this.nameButtonGoal, null));
        inventory.setItem(3, this.createItemStack(Material.VINE, 1, false, this.nameButtonName, null));
        inventory.setItem(6, this.createItemStack(Material.LAVA_BUCKET, 1, false, this.nameButtonDelete, null));
        inventory.setItem(8, this.createItemStack(Material.BARRIER, 1, false, this.nameButtonClose, null));
        player.openInventory(inventory);
        this.openMenus.put(player.getUniqueId(), parkourCourse);
    }

    public ItemStack createEditTool() {
        return this.createItemStack(this.materialEditTool, 1, true, this.nameEditTool, this.loreEditTool);
    }

    public ItemStack createPlayingTool() {
        return this.createItemStack(this.materialPlayingTool, 1, true, this.namePlayingTool, this.lorePlayingTool);
    }

    private ItemStack createItemStack(Material material, int amount, boolean unbreakable, String displayName, List<String> lore) {
        ItemStack tool = new ItemStack(material, amount);
        ItemMeta itemMeta = tool.getItemMeta();
        if (unbreakable)
            itemMeta.setUnbreakable(unbreakable);
        itemMeta.setDisplayName(displayName);
        itemMeta.setLore(lore);
        tool.setItemMeta(itemMeta);
        return tool;
    }

    public static String locationToString(Location location) {
        return location.getWorld().getName() + ':' + location.getBlockX() + ':' + location.getBlockY() + ':' + location.getBlockZ();
    }

}
