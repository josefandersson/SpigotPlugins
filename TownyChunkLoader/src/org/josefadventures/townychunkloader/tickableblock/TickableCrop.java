package org.josefadventures.townychunkloader.tickableblock;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;

/**
 * Tickable block for wheat, potato, carrot, melon stem, pumpkin stem
 * and nether wart (with the latter having only 4 growth stages).
 */
public class TickableCrop extends TickableBlock {

    public TickableCrop(Block block) {
        super(block);
    }

    @Override
    public void doRandomTicks(int numRandomTicks) {
        Ageable ageable = ((Ageable) this.block.getBlockData());

        if (ageable.getAge() != ageable.getMaximumAge()) {
            ageable.setAge(Math.min(ageable.getAge() + numRandomTicks, ageable.getMaximumAge()));

            BlockState blockState = block.getState();
            blockState.setBlockData(ageable);
            blockState.update();
        }
    }

}
