package com.omsent.addon.mixin;

import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.systems.modules.render.Zoom;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.joml.Vector3d;
import org.joml.Matrix4fStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;

@Mixin(value = NametagUtils.class, remap = false)
public abstract class NametagUtilsMixin {
    @Shadow @Final private static Vector4f vec4;
    @Shadow @Final private static Vector4f mmMat4;
    @Shadow @Final private static Vector4f pmMat4;
    @Shadow @Final private static Vector3d camera;
    @Shadow @Final private static Vector3d cameraNegated;
    @Shadow @Final private static Matrix4f model;
    @Shadow @Final private static Matrix4f projection;
    @Shadow private static double windowScale;

    /**
     * @author osmuikosmh
     * @reason fixed NametagUtils bug
     */
    @Overwrite
    public static boolean to2D(Vector3d pos, double scale, boolean distanceScaling, boolean allowBehind) {
        Zoom zoom = Modules.get().get(Zoom.class);
        NametagUtils.scale = scale * zoom.getScaling();
        if (distanceScaling) {
            NametagUtils.scale *= NametagUtilsAccessor.invokeGetScale(pos);
        }

        vec4.set(cameraNegated.x + pos.x, cameraNegated.y + pos.y, cameraNegated.z + pos.z, 1);

        vec4.mul(model, mmMat4);
        mmMat4.mul(projection, pmMat4);

        boolean behind = pmMat4.w <= 0.f;

        if (behind && !allowBehind) return false;

        NametagUtilsAccessor.invokeToScreen(pmMat4);
        double x = pmMat4.x * Utils.getWindowWidth();
        double y = pmMat4.y * Utils.getWindowHeight();

        if (behind) {
            x = Utils.getWindowWidth() - x;
            y = Utils.getWindowHeight() - y;
        }

        if (Double.isInfinite(x) || Double.isInfinite(y)) return false;

        // pos.set(x / windowScale, Utils.getWindowHeight() - y / windowScale, allowBehind ? pmMat4.w : pmMat4.z);
        pos.set(x, Utils.getWindowHeight() - y, allowBehind ? pmMat4.w : pmMat4.z);
        return true;
    }
}
