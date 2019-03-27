package org.josefadventures.townychunkloader;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Random;

/**
 * Keeps track of a chunk that has blocks that are affected by Minecraft
 * random ticks. This is used when a chunk is unloaded and loaded.
 */
public class TickableChunk {

    private String chunkId;
    private String world;
    private int chunkX;
    private int chunkY;

    private HashMap<Integer, TickableBlock> blocks;
    private int randomTickRate = 3; // blocks per section per tick to be updated, Minecraft default is 3
    private double chanceMultiplier = 1; // to make random ticking faster or slower

    public TickableChunk() {
        this.blocks = new HashMap<>();
    }

    public void loadFromChunk(Chunk chunk) {
        for (int x = 0; x < 16; x++) {
            for (int section = 0; section < 16; section++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        Block block = chunk.getBlock(x, y * section, z);

                        // TODO: determine if block should be saved as a tickableBlock or not


                    }
                }
            }
        }
    }

    public Chunk getChunk() {
        return Bukkit.getServer()
                .getWorld(this.world)
                .getChunkAt(this.chunkX, this.chunkY);
    }

    /**
     * Execute random ticks on tickableBlocks in this chunk.
     * Chance of random tick per tick is randomTickRate/4096.
     * NOTE: This method will load the chunk if it's not loaded.
     * @param numTicks Number of game ticks to catch up. (Ticks since last called or chunk unloaded.)
     */
    public void doRandomTicks(int numTicks) {
        double chance = (this.randomTickRate * numTicks) / 4096.0 * this.chanceMultiplier;

        Chunk chunk = this.getChunk();
        Random random = new Random(942602341);

        for (TickableBlock tickableBlock : this.blocks.values()) {
            tickableBlock.doRandomTicks(chunk, (int) (chance * random.nextGaussian()));
        }
    }

}
