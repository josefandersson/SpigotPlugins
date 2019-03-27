package org.josefadventures.redstonegates;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.Random;

public class GateRANDOM extends Gate {

    // Any inputs, any order

    private Random random;

    public GateRANDOM(Block sign, BlockFace[] inputSides) {
        super(sign, inputSides);

        this.random = new Random();
    }

    @Override
    public boolean output(boolean[] inputValues) {
        for (boolean value : inputValues) {
            if (value) {
                return this.random.nextBoolean();
            }
        }
        return false;
    }

}
