package org.josefadventures.redstonegates;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class GateOR extends Gate {

    // Any inputs, any order

    public GateOR(Block sign, BlockFace[] inputSides) {
        super(2, sign, inputSides);
    }

    @Override
    public boolean output(boolean[] inputValues) {
        int i = 0;
        for (boolean value : inputValues) {
            System.out.println("Input value(" + i++ + "): " + value);
            if (value) {
                return true;
            }
        }
        return false;
    }

}
