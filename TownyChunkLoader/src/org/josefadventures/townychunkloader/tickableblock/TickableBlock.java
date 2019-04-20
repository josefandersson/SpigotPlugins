package org.josefadventures.townychunkloader.tickableblock;

import org.bukkit.block.Block;

/**
 * Keeps track of a block that is affected by Minecraft random
 * ticks. This is used when a chunk is unloaded and loaded.
 */
public abstract class TickableBlock {

    protected Block block;

    public TickableBlock(Block block) {
        this.block = block;
    }

    /**
     * Execute random ticks on block at this TickableBlock's relative position in chunk.
     * @param numRandomTicks Number of random ticks to apply to block. (NOT game ticks
     *                       since last call, rather random ticks calculated from chance.)
     */
    public abstract void doRandomTicks(int numRandomTicks);

}
