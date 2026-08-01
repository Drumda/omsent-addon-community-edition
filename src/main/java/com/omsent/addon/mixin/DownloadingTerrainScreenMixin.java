package com.omsent.addon.mixin;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import com.omsent.addon.modules.NoLoadScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.DrawContext;

@Mixin(DownloadingTerrainScreen.class)
public class DownloadingTerrainScreenMixin {
    private boolean isActive() {
        return NoLoadScreen.getInstance().isActive();
    }

    @Inject(
        method = "render",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onRenderMainHud(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (isActive()) {
            mc.setScreen(null);
            ci.cancel();
        }
    }
}
