package org.josefadventures.commandtriggers;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

public class CommandTriggers extends JavaPlugin implements Listener {

    private final String PERMISSION_CREATE = "commandtriggers.create";
    private final String PERMISSION_LIST = "commandtriggers.list";
    private final String PERMISSION_USE = "commandtriggers.use";

    private HashMap<Location, CommandTrigger> triggers;
    private HashMap<UUID, CommandTrigger> hasTool;
    private HashMap<UUID, Long> useTimeout;

    @Override
    public void onEnable() {
        this.triggers = new HashMap<>();
        this.hasTool = new HashMap<>();
        this.useTimeout = new HashMap<>();

        this.getConfig().addDefault("triggers", new String[] {});
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();

        List<String> rawTrigs = this.getConfig().getStringList("triggers");
        for (String rawTrig : rawTrigs) {
            CommandTrigger ct = CommandTrigger.fromString(rawTrig);
            if (ct != null && ct.satisfied) {
                this.triggers.put(ct.location, ct);
            }
        }
        this.getLogger().info("Loaded " + this.triggers.size() + " triggers.");

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        ArrayList<String> rawTrigs = new ArrayList<>();
        for (CommandTrigger ct : this.triggers.values()) {
            rawTrigs.add(ct.toString());
        }
        this.getConfig().set("triggers", rawTrigs);
        this.saveConfig();

        if (!this.hasTool.isEmpty()) {
            for (Player player : getServer().getOnlinePlayers()) {
                if (this.hasTool.containsKey(player.getUniqueId())) {
                    CommandTrigger ct = this.hasTool.get(player.getUniqueId());
                    if (ct.satisfied) {
                        this.triggers.put(ct.location, ct);
                    }

                    ItemStack is = player.getInventory().getItemInMainHand();
                    player.getInventory().remove(is);
                    this.hasTool.remove(player.getUniqueId());
                }
            }
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(cmd.getPermission())) {
            sender.sendMessage(ChatColor.DARK_RED + cmd.getPermissionMessage());
            return true;
        }

        if (label.equals("triggerlist")) {
            sender.sendMessage("Displaying " + this.triggers.size() + " triggers:");

            for (CommandTrigger ct : this.triggers.values()) {
                sender.sendMessage("- "
                        + ct.location.getWorld().getName() + ": ["
                        + ct.location.getBlockX() + ", "
                        + ct.location.getBlockY() + ", "
                        + ct.location.getBlockZ() + "]");
            }

            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is only available in-game.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(cmd.getUsage());
            return true;
        }

        if (this.isPlayerUsingTool(((Player) sender).getUniqueId())) {
            sender.sendMessage("You are already using a trigger creation tool. Drop it first.");
            return true;
        }

        String trigCmd = null;
        int i = 0;
        for (; i < args.length; i++) {
            String arg = args[i];
            if (trigCmd == null) {
                if (arg.startsWith("\"")) {
                    if (arg.length() == 1) {
                        trigCmd = "";
                    } else {
                        trigCmd = arg.substring(1);
                    }
                }
            } else {
                if (arg.endsWith("\"")) {
                    if (arg.length() != 1) {
                        trigCmd += " " + arg.substring(0, arg.length() - 1);
                    }
                    break;
                } else {
                    trigCmd += " " + arg;
                }
            }
        }

        if (trigCmd == null) {
            sender.sendMessage(cmd.getUsage());
            sender.sendMessage("Did you forget to put \" around the command?");
        }

        if (this.giveTriggerTool((Player) sender, trigCmd)) {
            sender.sendMessage("You got the trigger tool for command \"" + trigCmd + "\". Switch item in hand to remove tool.");
        } else {
            sender.sendMessage("Couldn't give you the tool.");
        }

        return true;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent ev) {
        if (this.isPlayerUsingTool(ev.getPlayer().getUniqueId())) {
            if (ev.getAction() == Action.RIGHT_CLICK_BLOCK) {
                CommandTrigger ct = this.hasTool.get(ev.getPlayer().getUniqueId());
                ct.setLocation(ev.getClickedBlock().getLocation());
                ev.getPlayer().sendMessage("Set location for CommandTrigger with command: \"" + ct.command + "\"");
            } else if (ev.getAction() == Action.LEFT_CLICK_BLOCK) {
                CommandTrigger ct = this.hasTool.get(ev.getPlayer().getUniqueId());
                ct.setLocation(null);
                ev.getPlayer().sendMessage("Removed location for CommandTrigger with command: \"" + ct.command + "\"");
            }
        }

        if (ev.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (this.triggers.containsKey(ev.getClickedBlock().getLocation())) {
                if (ev.getPlayer().hasPermission(PERMISSION_USE)) {
                    CommandTrigger ct = this.triggers.get(ev.getClickedBlock().getLocation());
                    this.playerUseTrigger(ev.getPlayer(), ct);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent ev) {
        if (this.isPlayerUsingTool(ev.getPlayer().getUniqueId())) {
            CommandTrigger ct = this.hasTool.get(ev.getPlayer().getUniqueId());
            if (ct.satisfied)
                this.triggers.put(ct.location, ct);

            ItemStack is = ev.getPlayer().getInventory().getItem(ev.getPreviousSlot());
            ev.getPlayer().getInventory().remove(is);
            this.hasTool.remove(ev.getPlayer().getUniqueId());
        }
    }

    private void playerUseTrigger(Player player, CommandTrigger trigger) {
        if (this.useTimeout.containsKey(player.getUniqueId())) {
            if (this.useTimeout.get(player.getUniqueId()) > System.currentTimeMillis()) {
                return;
            }
        }

        boolean op = player.isOp();

        try {
            player.setOp(true);
            this.getServer().dispatchCommand(player, trigger.command);
        } catch(Exception e) {
        } finally {
            player.setOp(op);
        }

        this.useTimeout.put(player.getUniqueId(), System.currentTimeMillis() + 250);
    }

    private boolean isPlayerUsingTool(UUID uuid) {
        return this.hasTool.containsKey(uuid);
    }

    // true if could give
    private boolean giveTriggerTool(Player player, String command) {
        if (player.getInventory().getItemInMainHand().getAmount() == 0) {
            ItemStack st = new ItemStack(Material.STICK);
            ItemMeta meta = st.getItemMeta();
            meta.setDisplayName("Trigger Creation Tool");
            meta.setLore(Arrays.asList("Commandss:", command));
            st.setItemMeta(meta);
            player.getInventory().setItemInMainHand(st);

            CommandTrigger ct = new CommandTrigger(command);
            this.hasTool.put(player.getUniqueId(), ct);
            return true;
        }
        return false;
    }



}

class CommandTrigger {

    public Location location = null;
    public String command;

    public boolean satisfied = false;

    public CommandTrigger(String command) {
        this.command = command;
    }

    public void setLocation(Location location) {
        this.location = location;
        this.satisfied = this.location != null;
    }

    public String toString() {
        if (this.satisfied) {
            return this.command + ";"
                    + this.location.getWorld().getName() + ";"
                    + this.location.getBlockX() + ";"
                    + this.location.getBlockY() + ";"
                    + this.location.getBlockZ();
        } else {
            return null;
        }
    }

    public static CommandTrigger fromString(String str) {
        if (str == null) return null;
        if (str.length() == 0) return null;

        String[] parts = str.split(";");

        try {
            String cmd = parts[0];
            World world = Bukkit.getWorld(parts[1]);
            int x = Integer.parseInt(parts[2]);
            int y = Integer.parseInt(parts[3]);
            int z = Integer.parseInt(parts[4]);

            if (world == null) {
                Bukkit.getServer().getPluginManager().getPlugin("CommandTriggers")
                        .getLogger().warning("[CommandTrigger] World doesn't exist: " + parts[1]);
                return null;
            }

            Location loc = new Vector(x, y, z).toLocation(world);
            CommandTrigger ct = new CommandTrigger(cmd);
            ct.setLocation(loc);

            return ct;
        } catch (Exception e) {
            return null;
        }
    }

}