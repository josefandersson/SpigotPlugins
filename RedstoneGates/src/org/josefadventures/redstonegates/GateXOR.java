package org.josefadventures.redstonegates;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class GateXOR extends Gate {

    // Two inputs, any order

    public GateXOR(Block sign, BlockFace[] inputSides) {
        super(sign, inputSides);

        if (this.getNumSides() != 2) {
            this.disable();
        }
    }

    @Override
    public boolean output(boolean[] inputValues) {
        return inputValues[0] != inputValues[1];
    }

}
