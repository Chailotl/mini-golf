package com.chai.miniGolf.utils;

import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Slab;

public class SharedMethods {

    public static boolean isBottomSlab(Block block) {
        return Tag.SLABS.isTagged(block.getType()) && ((Slab) block.getBlockData()).getType() == Slab.Type.BOTTOM;
    }
}
