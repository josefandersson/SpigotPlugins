package org.josefadventures.redstonegates;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class GateNAND extends Gate {

    // Any inputs, any order

    public GateNAND(Block sign, BlockFace[] inputSides) {
        super(2, sign, inputSides);
    }

    @Override
    public boolean output(boolean[] inputValues) {
        for (boolean value : inputValues) {
            if (!value) {
                return true;
            }
        }
        return false;
    }

}
