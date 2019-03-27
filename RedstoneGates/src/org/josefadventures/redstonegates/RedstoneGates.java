package org.josefadventures.redstonegates;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class RedstoneGates extends JavaPlugin implements Listener {

    public final String PERMISSION_CREATE = "redstonegates.create";

    private ArrayList<Gate> gates;
    private ArrayList<Gate> loadedGates;

    private byte tickTracker = 0;
    private byte lastUpdateTick = 0;

    @Override
    public void onEnable() {
        this.gates = new ArrayList<>();
        this.loadedGates = new ArrayList<>();

        // TODO: load saved gates

        this.getServer().getPluginManager().registerEvents(this, this);

        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            this.tickTracker++;
        }, 0, 1);
    }

    @Override
    public void onDisable() {
        // TODO: save gates
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onSignChange(SignChangeEvent ev) {
        if (ev.getLine(0).equalsIgnoreCase("[gate]")) {
            if (ev.getPlayer().hasPermission(PERMISSION_CREATE)) {
                if (ev.getLine(1).length() != 0) {

                    BlockFace[] inputSides = Gate.ALL_SIDES;
                    if (ev.getLine(2).length() != 0) {
                        String[] sides = ev.getLine(2).split(",");
                        inputSides = new BlockFace[sides.length];
                        for (int i = 0; i < sides.length; i++) {
                            System.out.println("Checking BlockFace of side: " + sides[i]);
                            switch (sides[i].trim().toLowerCase()) {
                                case "u":
                                case "up":
                                    inputSides[i] = BlockFace.UP;
                                    break;
                                case "d":
                                case "down":
                                    inputSides[i] = BlockFace.DOWN;
                                    break;
                                case "n":
                                case "north":
                                    inputSides[i] = BlockFace.NORTH;
                                    break;
                                case "s":
                                case "south":
                                    inputSides[i] = BlockFace.SOUTH;
                                    break;
                                case "e":
                                case "east":
                                    inputSides[i] = BlockFace.EAST;
                                    break;
                                case "w":
                                case "west":
                                    inputSides[i] = BlockFace.WEST;
                                    break;
                                case "f":
                                case "forward":
                                case "forwards":
                                    // TODO: calc
                                    break;
                                case "b":
                                case "backward":
                                case "backwards":
                                    // TODO: calc
                                    break;
                                case "l":
                                case "left":
                                    // TODO: calc
                                    break;
                                case "r":
                                case "right":
                                    // TODO: calc
                                    break;
                                default:
                                    System.out.println("OUCH");
                                    return;
                            }
                        }
                    }

                    Gate newGate = null;
                    String name = ev.getLine(1).toUpperCase();
                    switch (name) {
                        case "AND":
                            newGate = new GateAND(ev.getBlock(), inputSides);
                            break;
                        case "NAND":
                            newGate = new GateNAND(ev.getBlock(), inputSides);
                            break;
                        case "OR":
                            newGate = new GateOR(ev.getBlock(), inputSides);
                            break;
                        case "NOR":
                            newGate = new GateNOR(ev.getBlock(), inputSides);
                            break;
                        case "XOR":
                            newGate = new GateXOR(ev.getBlock(), inputSides);
                            break;
                        case "XNOR":
                            newGate = new GateXNOR(ev.getBlock(), inputSides);
                            break;
                        case "RANDOM":
                            newGate = new GateRANDOM(ev.getBlock(), inputSides);
                            break;
                        default:
                            ev.getPlayer().sendMessage(ChatColor.RED + "Unknown gate \"" + ev.getLine(1) + "\".");
                            ev.setLine(0, ChatColor.DARK_RED + "[Gate]");
                            ev.setLine(1, ChatColor.DARK_RED + ev.getLine(1));
                            return;
                    }
                    if (newGate.isEnabled()) {
                        this.gates.add(newGate);
                        ev.setLine(0, ChatColor.DARK_BLUE + "[Gate]");
                        ev.setLine(1, ChatColor.BLUE + name);
                    } else {
                        ev.getPlayer().sendMessage(ChatColor.RED + "Gate couldn't be created. Is the output free of blocks?");
                        ev.setLine(0, ChatColor.DARK_RED + "[Gate]");
                        ev.setLine(1, ChatColor.DARK_RED + name);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent ev) {
        // TODO: move gates from unloaded list
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent ev) {
        // TODO: move gates to unloaded list
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent ev) {
        for (Gate gate : this.gates) {
            if (gate.isClose(ev.getBlock().getLocation())) {
                if (gate.isBase(ev.getBlock())
                        || gate.isSign(ev.getBlock())) {
                    this.gates.remove(gate);
                    gate.delete();
                    ev.getPlayer().sendMessage(ChatColor.GREEN + "Gate was deleted.");
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockRedstone(BlockRedstoneEvent ev) {
        if (this.tickTracker != this.lastUpdateTick) {
            this.lastUpdateTick = this.tickTracker;
            this.getServer().getScheduler().runTaskLater(this, () -> {
                for (Gate gate : this.gates) {
                    if (gate.isClose(ev.getBlock().getLocation())) {
                        gate.update();
                    }
                }
            }, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent ev) {
        if (ev.getClickedBlock() != null) {
            if (ev.getClickedBlock().getType().equals(Material.WALL_SIGN)) {
                for (Gate gate : this.gates) {
                    if (gate.isClose(ev.getClickedBlock().getLocation())) {
                        if (gate.isSign(ev.getClickedBlock())) {
                            ev.getPlayer().sendMessage(ChatColor.DARK_GREEN + "Info about gate:");
                            String sides = "";
                            for (BlockFace face : gate.getInputSides()) {
                                sides += face.name() + '_' + gate.getBase().getBlockPower(face)
                                        + '_' + gate.getBase().isBlockFaceIndirectlyPowered(face)
                                        + '_' + gate.getBase().isBlockFacePowered(face)
                                        + '_' + gate.getBase().getRelative(face).getBlockPower()
                                        + '_' + gate.getBase().getRelative(face).isBlockPowered()
                                        + '_' + gate.getBase().getRelative(face).isBlockFacePowered(face)
                                        + '_' + gate.getBase().getRelative(face).isBlockFacePowered(face.getOppositeFace()) + ' ';
                            }
                            ev.getPlayer().sendMessage(ChatColor.GREEN + "Input sides (" + ChatColor.DARK_GREEN + gate.getNumSides()
                                    + ChatColor.GREEN + "): " + ChatColor.DARK_GREEN + sides);
                            gate.update();
                        }
                    }
                }
            }
        }
    }

}
