package com.omsent.addon.mixin;

import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.Utils;
import org.joml.Vector4f;
import org.joml.Vector3d;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = NametagUtils.class, remap = false)
public interface NametagUtilsAccessor {
    @Invoker("getScale")
    static double invokeGetScale(Vector3d pos) { throw new AssertionError(); }

    @Invoker("toScreen")
    static void invokeToScreen(Vector4f vec) { throw new AssertionError(); }
}
