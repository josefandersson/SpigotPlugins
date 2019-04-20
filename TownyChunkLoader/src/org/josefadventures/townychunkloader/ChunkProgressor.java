package org.josefadventures.townychunkloader;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.josefadventures.townychunkloader.tickableblock.*;

import java.util.ArrayList;
import java.util.Random;

/**
 * Progresses a chunk with X random ticks.
 */
public class ChunkProgressor {

    private static final int RANDOM_TICK_RATE = 3; // Minecraft standard

    private Chunk chunk;
    private ArrayList<TickableBlock> blocks;
    private int index = 0;

    private Random random;
    private double randomTickChance;

    /**
     * Begin to progress a chunk.
     * @param chunk chunk to progress.
     * @param numTicks number of game ticks to progress.
     * @param multiplier increase or decrease rate of random ticking. (<1 = dec, >1 = inc)
     */
    public ChunkProgressor(Chunk chunk, int numTicks, double multiplier) {
        this.chunk = chunk;

        this.random = new Random();
        this.randomTickChance = RANDOM_TICK_RATE / 4096.0 * multiplier * numTicks;

        // Find tickable blocks asynchronously for good measure
        Bukkit.getScheduler().runTaskAsynchronously(TownyChunkLoader.instance, () -> {
            this.findTickableBlocks();
            this.scheduleNext();
        });
    }

    private void scheduleNext() {
        Bukkit.getScheduler().runTaskLater(TownyChunkLoader.instance, () -> {
            if (this.updateTickableBlocks()) {
                this.scheduleNext();
            }
        }, 1);
    }

    private int randomizeTicks() {
        return (int) (this.randomTickChance * Math.abs(this.random.nextGaussian()));
    }

    /**
     * Apply random ticks to X amount of tickable blocks.
     * @return whether there are more blocks to be updated (i.e. this method has to be called again)
     */
    private boolean updateTickableBlocks() {
        int max = Math.min(this.index + TownyChunkLoader.instance.maxUpdatesPerTick, this.blocks.size());
        int numRandomTicks;
        for (; this.index < max; this.index++) {
            numRandomTicks = this.randomizeTicks();
            if (numRandomTicks != 0) {
                this.blocks.get(this.index).doRandomTicks(numRandomTicks);
            }
        }

        return this.index < this.blocks.size() - 1;
    }

    /**
     * Go through each block in chunk and convert tickable blocks to TickableBlock.
     */
    private void findTickableBlocks() { // TODO: This can be done way more easy on the system asynchronously, takes around 10-15ms
        this.blocks = new ArrayList<>();

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    switch (this.chunk.getBlock(x, y, z).getType()) {
                        case NETHER_WART:
                        case PUMPKIN_STEM:
                        case MELON_STEM:
                        case CARROT:
                        case POTATO:
                        case WHEAT:
                            this.blocks.add(new TickableCrop(this.chunk.getBlock(x, y, z)));
                            break;

                        case SPRUCE_SAPLING:
                        case ACACIA_SAPLING:
                        case BIRCH_SAPLING:
                        case DARK_OAK_SAPLING:
                        case JUNGLE_SAPLING:
                        case OAK_SAPLING:
                            this.blocks.add(new TickableSapling(this.chunk.getBlock(x, y, z)));
                            break;

                        case SUGAR_CANE:
                            this.blocks.add(new TickableCaneCacti(this.chunk.getBlock(x, y, z), 2));
                            break;

                        case CACTUS:
                            this.blocks.add(new TickableCaneCacti(this.chunk.getBlock(x, y, z), 1));
                            break;

                        // TODO: leaves(!), mushrooms, vines, chorus, sea pickle, coral?
                    }
                }
            }
        }
    }

}
