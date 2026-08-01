package com.omsent.addon.mixin;

import com.omsent.addon.modules.Info;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.systems.modules.Module;

@Mixin(value = Module.class, remap = false)
public class ModuleMixin {
    @Shadow public String name;
    @Shadow private boolean active;

    @Inject(
        method = "sendToggledMsg",
        at = @At("HEAD")
    )
    private void sendToggledMsg(CallbackInfo ci) {
        Info.getInstance().addModuleNotify(name, active);
    }
}
