package com.tpexcotblu.litematicgen.generators;

import com.tpexcotblu.litematicgen.schematic.SchematicVolume;

public interface SchemeGenerator {
    SchematicVolume generate();
    String getFilename();
    String getSuccessMessage();
}
