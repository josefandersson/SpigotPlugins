package org.josefadventures.townychunkloader;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

/**
 * Keeps track of a block that is affected by Minecraft random
 * ticks. This is used when a chunk is unloaded and loaded.
 */
public class TickableBlock {

    private int position; // in chunk starting from 0,0,0 and incrementing one per block with formula x*4096,y*16,z
    private int section; // chunks are divided into 16 sections 16 blocks high

    private TickableBlockType tickableBlockType;

    public TickableBlock(TickableBlockType tickableBlockType, Block block) {
        this.tickableBlockType = tickableBlockType;
        this.position = locationToIntPosition(block.getLocation());
    }

    public TickableBlock(TickableBlockType tickableBlockType, int position, int section) {
        this.tickableBlockType = tickableBlockType;
        this.position = position;
        this.section = section;
    }

    /**
     * Get block from chunk from this TickableBlock's relative position.
     * @param chunk Chunk to get block from.
     * @return Block in chunk.
     */
    public Block getBlock(Chunk chunk) {
        Vector relativePosition = intPositionToVector(this.position);
        return chunk.getBlock(relativePosition.getBlockX(),
                relativePosition.getBlockY(),
                relativePosition.getBlockZ());
    }

    /**
     * Execute random ticks on block at this TickableBlock's relative position in chunk.
     * @param chunk Chunk where block is.
     * @param numRandomTicks Number of random ticks to apply to block. (NOT game ticks
     *                       since last call, rather random ticks calculated from chance.)
     */
    public void doRandomTicks(Chunk chunk, int numRandomTicks) {
        Block block = this.getBlock(chunk);
        
        // TODO: different depending on tickableType

    }

    public static int locationToIntPosition(Location location) {
        Chunk chunk = location.getChunk();
        return (location.getBlockX() - chunk.getX() * 16) * 4096
                + location.getBlockY() * 16
                + location.getBlockZ() - chunk.getZ() * 16;

    }

    public static Vector intPositionToVector(int position) {
        int x = position / 4096;
        position %= 4096;
        int y = position / 16;

        return new Vector(x, y, position % 16);
    }

}
