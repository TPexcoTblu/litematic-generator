package com.tpexcotblu.litematicgen.mixin;

import com.tpexcotblu.litematicgen.client.GeneratorHotkeys;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void litematicgen$pollKey(CallbackInfo ci) { GeneratorHotkeys.tick(); }
}
