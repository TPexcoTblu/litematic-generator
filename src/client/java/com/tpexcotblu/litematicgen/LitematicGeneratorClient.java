package com.tpexcotblu.litematicgen;

import com.tpexcotblu.litematicgen.commands.SchemeCommandRegistrar;
import net.fabricmc.api.ClientModInitializer;

public class LitematicGeneratorClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SchemeCommandRegistrar.registerAllCommands();
	}
}