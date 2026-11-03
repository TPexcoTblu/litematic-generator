package com.tpexcotblu.litematicgen.mixin;

import com.tpexcotblu.litematicgen.client.GeneratorHotkeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Arrays;

@Mixin(Options.class)
public abstract class OptionsMixin {
    @Shadow @Final @Mutable public KeyMapping[] keyMappings;

    // Register before options.txt is read, so a saved custom binding survives restart.
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;load()V"))
    private void litematicgen$registerKey(CallbackInfo ci) {
        for (KeyMapping mapping : keyMappings) {
            if (mapping == GeneratorHotkeys.OPEN) return;
        }
        keyMappings = Arrays.copyOf(keyMappings, keyMappings.length + 1);
        keyMappings[keyMappings.length - 1] = GeneratorHotkeys.OPEN;
    }
}
