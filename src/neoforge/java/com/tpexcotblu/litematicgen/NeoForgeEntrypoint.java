package com.tpexcotblu.litematicgen;
import com.tpexcotblu.litematicgen.client.GeneratorSettingsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
@Mod(value = "litematic_generator", dist = Dist.CLIENT)
public final class NeoForgeEntrypoint {
    public NeoForgeEntrypoint(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> new GeneratorSettingsScreen(parent));
    }
}
