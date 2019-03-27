package org.josefadventures.redstonegates;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class GateXNOR extends Gate {

    // Any inputs > 2, any order

    public GateXNOR(Block sign, BlockFace[] inputSides) {
        super(2, sign, inputSides);
    }

    @Override
    public boolean output(boolean[] inputValues) {
        boolean normal = inputValues[0];
        for (int i = 1; i < this.getNumSides(); i++) {
            if (inputValues[i] != normal) {
                return false;
            }
        }
        return true;
    }

}
