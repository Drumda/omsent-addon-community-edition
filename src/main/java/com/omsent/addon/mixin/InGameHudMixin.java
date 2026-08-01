package com.omsent.addon.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import com.omsent.addon.modules.Info;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.gui.DrawContext;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    private boolean isToggle() {
        return Info.getInstance().isActive() &&
            Info.getInstance().toggleHud.get();
    }

    @Inject(
        method = "renderMainHud(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onRenderMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (isToggle()) ci.cancel();
    }
    @Inject(
        method = "renderExperienceLevel(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onRenderExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (isToggle()) ci.cancel();
    }
}
