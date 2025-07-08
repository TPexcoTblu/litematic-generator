package com.tpexcotblu.litematicgen.generators;

import net.minecraft.nbt.NbtList;

public interface SchemeGenerator {
    NbtList generateBlocks();
    NbtList generateSize();
    String getFilename();
    String getSuccessMessage();
}
