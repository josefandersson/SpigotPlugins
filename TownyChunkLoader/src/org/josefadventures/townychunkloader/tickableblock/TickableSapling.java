package org.josefadventures.townychunkloader.tickableblock;

import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Sapling;

import java.util.Random;

public class TickableSapling extends TickableBlock {

    public TickableSapling(Block block) {
        super(block);
    }

    @Override
    public void doRandomTicks(int numRandomTicks) {
        Sapling sapling = (Sapling) this.block.getBlockData();

        int delta = numRandomTicks;

        if (sapling.getStage() != sapling.getMaximumStage()) {
            delta --;

            sapling.setStage(1);
            BlockState blockState = block.getState();
            blockState.setBlockData(sapling);
            blockState.update(true, true);
        }

        if (delta != 0) {
            TreeType treeType;

            switch (this.block.getType()) {
                case OAK_SAPLING:
                    if (new Random().nextDouble() < .1)
                        treeType = TreeType.BIG_TREE;
                    else
                        treeType = TreeType.TREE;
                    break;

                case SPRUCE_SAPLING:
                    if (new Random().nextDouble() < .3)
                        treeType = TreeType.TALL_REDWOOD;
                    else
                        treeType = TreeType.REDWOOD;
                    break;

                case BIRCH_SAPLING:
                    treeType = TreeType.BIRCH;
                    break;

                case ACACIA_SAPLING:
                    treeType = TreeType.ACACIA;
                    break;

                // TODO: 2x2 trees...
                case JUNGLE_SAPLING:
                case DARK_OAK_SAPLING:

                default:
                    treeType = TreeType.TREE;
            }

            for (; 0 < delta; delta--) {
                if (this.block.getWorld().generateTree(this.block.getLocation(), treeType)) {
                    this.block.setType(this.block.getRelative(0, 1, 0).getType());
                }
            }
        }

    }

}
