package org.josefadventures.redstonegates;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Redstone;
import org.bukkit.material.RedstoneWire;
import org.bukkit.material.Sign;
import org.bukkit.util.Vector;

public abstract class Gate {

    public final static BlockFace[] ALL_SIDES = new BlockFace[] {
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST
    };

    private boolean enabled = true;
    private int numSides;

    private Block sign;
    private Block base;
    private Block output;

    private BlockFace[] inputSides;
    private boolean[] inputValues;

    public boolean isEnabled() { return this.enabled; }
    public void disable() { this.enabled = false; }

    public int getNumSides() { return this.numSides; }
    public BlockFace[] getInputSides() { return this.inputSides; }
    public Block getBase() { return this.base; }

    public Gate(Block sign) {
        this(1, sign);
    }

    public Gate(int minimumNumSides, Block sign) {
        this(minimumNumSides, sign, ALL_SIDES);
    }

    public Gate(Block sign, BlockFace[] inputSides) {
        this(1, sign, inputSides);
    }

    public Gate(int minimumNumSides, Block sign, BlockFace[] inputSides) {
        this.sign = sign;
        this.inputSides = inputSides;
        this.numSides = inputSides.length;
        this.inputValues = new boolean[this.numSides];

        if (minimumNumSides <= this.numSides) {
            Sign signData = (Sign) sign.getState().getData();
            Vector facing = signData.getFacing().getDirection();

            this.base = this.sign.getLocation().subtract(facing).getBlock();
            this.output = this.base.getLocation().subtract(facing).getBlock();

            if (this.output.getType() != Material.AIR) {
                this.disable();
            } else {
                this.update();
            }
        }
    }

    public boolean isBase(Block block) {
        return this.base.equals(block);
    }

    public boolean isSign(Block block) {
        return this.sign.equals(block);
    }

    public void delete() {
        this.sign.setType(Material.AIR);
        this.output.setType(Material.AIR);

        this.sign = null;
        this.base = null;
        this.output = null;
    }

    public boolean isClose(Location location) {
        return location.distance(this.base.getLocation()) < 3;
    }

    public void update() {
        if (this.isEnabled()) {
            boolean shouldLit = this.output();
            if (shouldLit) {
                this.output.setType(Material.REDSTONE_BLOCK);
            } else {
                this.output.setType(Material.STONE);
            }
        }
    }

    public boolean output() {
        for (int i = 0; i < this.inputSides.length; i++) {
            Block relative = this.base.getRelative(this.inputSides[i]);
            MaterialData materialData = relative.getState().getData();
            if (materialData instanceof Redstone) {
                BlockData blockData = relative.getBlockData();
                if (blockData instanceof Directional) {
                    System.out.println("BlockData is directional");
                    if (((Directional) blockData).getFacing() == this.inputSides[i]) {
                        this.inputValues[i] = ((Redstone) materialData).isPowered();
                    } else {
                        this.inputValues[i] = false;
                    }
                } else {
                    this.inputValues[i] = ((Redstone) materialData).isPowered();
                }

                /*if (materialData instanceof RedstoneWire) {
                    ((org.bukkit.block.data.type.RedstoneWire) relative.getBlockData()).
                }
                this.inputValues[i] = ((Redstone) materialData).isPowered();*/
            } else {
                this.inputValues[i] = false;
            }
        }

        return this.output(this.inputValues);
    }

    public abstract boolean output(boolean[] inputValues);

    // if side
    //    redstone wire (direction),

    // base NOT powered, but relative IS powered -> relative has incoming power
    private boolean valueOnFace(BlockFace blockFace) {
        Block relative = this.base.getRelative(blockFace);
        BlockData blockData = relative.getBlockData();
        MaterialData materialData = relative.getState().getData();
        return true;
    }

    private static void isBlockAffectingBlock(Block block, BlockFace blockFace) {

    }

}
