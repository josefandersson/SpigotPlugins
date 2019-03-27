package org.josefadventures.townychunkloader;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.material.*;

import java.util.ArrayList;
import java.util.UUID;

public class ChunkLoaderv1 {
    String world;
    int x;
    int z;
    UUID ownerUUID;
    int hours;
    long nextTimeout;
    long unloadTime;

    private ArrayList<Block> growables;
    LoadState loadState = LoadState.LOADED;

    public ChunkLoaderv1(Chunk chunk, UUID ownerUUID, int hours) {
        this.world = chunk.getWorld().getName();
        this.x = chunk.getX();
        this.z = chunk.getZ();
        this.ownerUUID = ownerUUID;
        this.hours = hours;
        this.nextTimeout = System.currentTimeMillis() + this.hours * 3600000;

        this.growables = new ArrayList<>();
    }

    public ChunkLoaderv1(Chunk chunk, UUID ownerUUID, int hours, long nextTimeout) {
        this.world = chunk.getWorld().getName();
        this.x = chunk.getX();
        this.z = chunk.getZ();
        this.ownerUUID = ownerUUID;
        this.hours = hours;
        this.nextTimeout = nextTimeout;

        this.growables = new ArrayList<>();
    }

    private ChunkLoaderv1(String world, int x, int z, UUID ownerUUID, int hours, long nextTimeout) {
        this.world = world;
        this.x = x;
        this.z = z;
        this.ownerUUID = ownerUUID;
        this.hours = hours;
        this.nextTimeout = nextTimeout;

        this.growables = new ArrayList<>();
    }


    public void updateCropsList() {
        if (this.loadState != LoadState.LOADED) return;

        this.loadState = LoadState.PRE_UNLOADED;

        Bukkit.getServer().getScheduler().runTaskAsynchronously(TownyChunkLoader.instance, () -> {
            long sartTime = System.currentTimeMillis();
            ArrayList<Block> blocks = new ArrayList<>();
            Chunk chunk = this.getChunk();
            for (int x = 0; x < 16; x++) {
                for (int y = 1; y < 256; y++) {
                    for (int z = 0; z < 16; z++) {
                        Block block = chunk.getBlock(x, y, z);
                        MaterialData materialData = block.getState().getData();
                        if (materialData instanceof Crops
                                || materialData instanceof Sapling
                                || materialData instanceof CocoaPlant
                                || materialData instanceof NetherWarts
                                || materialData instanceof Leaves
                                || block.getType().getId() == 83) { // Sugar cane
                            blocks.add(block);
                        }
                    }
                }
            }
            this.growables = blocks;
            System.out.println("updateCropsList done took " + (System.currentTimeMillis() - sartTime) + "ms, added " + blocks.size() + " blocks.");

            this.loadState = LoadState.UNLOADED;
        });
    }

    public void growCropsSinceUnload() {
        if (this.loadState != LoadState.UNLOADED) return;

        //this.loadState = LoadState.PRE_LOADED;

        long catchupTime = Math.min(System.currentTimeMillis(), this.nextTimeout) - this.unloadTime;
        System.out.println("We have " + catchupTime + "ms to catch up.");

        int states = (int) catchupTime / 60000;
        System.out.println("Growing crops " + states + " states.");

        for (Block block : this.growables) {
            // TODO: grow(block, states);
            grow(block, 1);
        }

        //this.loadState = LoadState.LOADED;
    }

    public static void grow(Block block, int statesToGrow) {

        BlockData blockData = block.getBlockData();
        if (blockData instanceof Ageable) {
            Ageable ageable = ((Ageable) blockData);
            ageable.setAge(Math.min(ageable.getAge() + statesToGrow, ageable.getMaximumAge()));

            BlockState blockState = block.getState();
            blockState.setBlockData(ageable);
            blockState.update();
        } else if (blockData instanceof org.bukkit.block.data.type.Sapling) {
            org.bukkit.block.data.type.Sapling sapling = (org.bukkit.block.data.type.Sapling) blockData;
            System.out.println("Updating a sapling on stage " + sapling.getStage() + " with max stage " + sapling.getMaximumStage());
            sapling.setStage(Math.min(sapling.getStage() + statesToGrow, sapling.getMaximumStage()));
            BlockState blockState = block.getState();
            blockState.setBlockData(sapling);
            blockState.update(true, true);
        }

        /*MaterialData data = block.getState().getData();

        if (data instanceof Crops) {
            if (((Crops) data).getState() != CropState.RIPE) {
                block.setType(Material.LEGACY_CROPS);
                Crops crops = new Crops(CropState.values()[Math.min(((Crops) data).getState().ordinal() + statesToGrow, 8)]);
                BlockState blockState = block.getState();
                blockState.setData(crops);
                blockState.update();
            }
        }*/
    }

    // TODO: Load chunk if it's unloaded.
    public void bump() {
        this.nextTimeout = System.currentTimeMillis() + this.hours * 3600000;
    }

    public String id() {
        return this.world + ";"
                + this.x + ";"
                + this.z;
    }

    public boolean isActive() {
        return this.nextTimeout > System.currentTimeMillis();
    }

    public Chunk getChunk() {
        World world = Bukkit.getWorld(this.world);
        if (world != null) {
            return world.getChunkAt(this.x, this.z);
        }
        return null;
    }

    public OfflinePlayer getOwner() {
        OfflinePlayer player = Bukkit.getPlayer(this.ownerUUID);
        if (player == null) {
            player = Bukkit.getOfflinePlayer(this.ownerUUID);
        }
        return player;
    }

    public String toInfoString() {
        OfflinePlayer player = Bukkit.getOfflinePlayer(this.ownerUUID);
        String str = "Created by " + player.getName() + ".";
        if (this.nextTimeout <= System.currentTimeMillis()) {
            str += " Currently inactive.";
        } else {
            long delta = (this.nextTimeout - System.currentTimeMillis()) / 60000;
            if (delta > 0) {
                str += " Needs a bump in ";
                int hours = (int) delta / 60;
                int minutes = (int) delta - hours * 60;
                if (hours != 0) {
                    str += hours + " hour";
                    if (hours > 1) str += "s";
                    if (minutes != 0) {
                        str += " and " + minutes + " minute";
                        if (minutes > 1) str += "s";
                    }
                    str += ".";
                } else {
                    str += minutes + " minute";
                    if (minutes > 1) str += "s";
                    str += ".";
                }
            }
        }

        return str;
    }

    // For saving
    public String toString() {
        return this.world + ";"
                + this.x + ";"
                + this.z + ";"
                + this.ownerUUID.toString() + ";"
                + this.hours + ";"
                + this.nextTimeout;
    }

    public static String chunkId(Chunk chunk) {
        if (chunk == null) return null;

        return chunk.getWorld().getName() + ";"
                + chunk.getX() + ";"
                + chunk.getZ();
    }

    public static String chunkIdFromVaribles(World world, int x, int z) {
        if (world == null) return null;

        return world.getName() + ";"
                + x + ";"
                + z;
    }

    // For loading
    public static ChunkLoaderv1 fromString(String str) {
        if (str == null) return null;

        String[] parts = str.split(";");

        try {
            String world = parts[0];
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            UUID ownerUUID = UUID.fromString(parts[3]);
            int hours = Integer.parseInt(parts[4]);
            long nextTimeout = Long.parseLong(parts[5]);

            return new ChunkLoaderv1(world, x, z, ownerUUID, hours, nextTimeout);
        } catch (Exception e) {
            return null;
        }
    }

    public enum LoadState {
        PRE_LOADED, LOADED, PRE_UNLOADED, UNLOADED
    }

}
