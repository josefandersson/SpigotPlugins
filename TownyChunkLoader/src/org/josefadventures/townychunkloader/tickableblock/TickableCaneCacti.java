package org.josefadventures.townychunkloader.tickableblock;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;

/**
 * Tickable block for sugar cane and cacti.
 *
 * Given height (max-of-type below this block) of 2 as default for canes and cacti.
 */
public class TickableCaneCacti extends TickableBlock {

    private int height;
    private Material material;

    public TickableCaneCacti(Block block, int height) {
        super(block);
        this.height = height;
        this.material = this.block.getType();
    }

    @Override
    public void doRandomTicks(int numRandomTicks) {
        if (this.block.getType() != this.material) // maybe a piston has been triggered and removed this block?
            return;

        Ageable ageable = ((Ageable) this.block.getBlockData());

        int delta = numRandomTicks;

        if (ageable.getAge() != ageable.getMaximumAge()) {
            int diff = ageable.getMaximumAge() - ageable.getAge();
            delta -= diff;

            ageable.setAge(ageable.getAge() + diff);

            BlockState blockState = block.getState();
            blockState.setBlockData(ageable);
            blockState.update();
        }

        if (delta != 0) {
            if (!this.block.getRelative(0, 1, 0).getType().equals(Material.AIR)) {
                return;
            }

            if (this.isMaxed()) {
                return;
            }

            Block blockAbove = this.block.getRelative(0, 1, 0);
            blockAbove.setType(this.block.getType());
            new TickableCaneCacti(blockAbove, this.height).doRandomTicks(delta - 1);
        }
    }

    private boolean isMaxed() {
        for (int i = 1; i <= this.height; i++) {
            if (!this.block.getRelative(0, -i, 0).getType().equals(this.block.getType())) {
                return false;
            }
        }
        return true;
    }

}
