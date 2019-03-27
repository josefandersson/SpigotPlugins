package org.josefadventures.redstonegates;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class GateNOR extends Gate {

    // Any inputs, any order

    public GateNOR(Block sign, BlockFace[] inputSides) {
        super(2, sign, inputSides);
    }

    @Override
    public boolean output(boolean[] inputValues) {
        for (boolean value : inputValues) {
            if (value) {
                return false;
            }
        }
        return true;
    }

}
