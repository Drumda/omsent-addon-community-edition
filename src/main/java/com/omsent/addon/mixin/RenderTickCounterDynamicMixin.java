/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package com.omsent.addon.mixin;

import com.omsent.addon.modules.AutoUseTimer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.RenderTickCounter;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTickCounter.Dynamic.class)
public abstract class RenderTickCounterDynamicMixin {
    @Shadow
    private float lastFrameDuration;
    @Inject(method = "beginRenderTick(J)I", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/RenderTickCounter$Dynamic;prevTimeMillis:J", opcode = Opcodes.PUTFIELD))
    private void onBeingRenderTick(long a, CallbackInfoReturnable<Integer> info) {
        if(Modules.get().get(AutoUseTimer.class).checkplayer() && Modules.get().get(AutoUseTimer.class).isActive() && !Modules.get().get(AutoUseTimer.class).FriendTimer.get()) {
            lastFrameDuration *= (float) Modules.get().get(AutoUseTimer.class).getMultiplier();
        } else if (Modules.get().get(AutoUseTimer.class).isActive() && Modules.get().get(AutoUseTimer.class).FriendTimer.get() && Modules.get().get(AutoUseTimer.class).checkplayer_forfriend()) {
            lastFrameDuration *= (float) Modules.get().get(AutoUseTimer.class).getMultiplier_Friend();
        }
    }
}
