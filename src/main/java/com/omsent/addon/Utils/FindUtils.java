package com.omsent.addon.Utils;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.ShapeContext;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.EntityType;

import java.util.function.BiFunction;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

public class FindUtils {
    private FindUtils() {}

    public static final record LambdaStopStatus(Object value) {}

    public static Object blockIterator(BlockPos blockPos, int range, BiFunction<BlockPos, BlockState, LambdaStopStatus> func) {
        int eMinX = blockPos.getX() - range;
        int eMaxX = blockPos.getX() + range;
        int eMinZ = blockPos.getZ() - range;
        int eMaxZ = blockPos.getZ() + range;

        int cStartX = eMinX >> 4;
        int cStartZ = eMinZ >> 4;
        int cEndX = eMaxX >> 4;
        int cEndZ = eMaxZ >> 4;

        for (int cX = cStartX; cX <= cEndX; cX++) {
            for (int cZ = cStartZ; cZ <= cEndZ; cZ++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cX, cZ, false);
                if (chunk == null) continue;

                int startX = cX << 4;
                int startZ = cZ << 4;

                int yMin = Math.max(mc.world.getBottomY(), blockPos.getY() - range);
                int yMax = Math.min(mc.world.getHeight() - 1, blockPos.getY() + range);

                for (int y = yMin; y <= yMax; y++) {
                    for (int z = startZ; z < startZ + 16; z++) {
                        for (int x = startX; x < startX + 16; x++) {
                            BlockPos bp = new BlockPos(x, y, z);
                            if (blockPos.toCenterPos().distanceTo(bp.toCenterPos()) > range) continue;

                            Object funcResult = func.apply(bp, chunk.getBlockState(bp));
                            if (funcResult != null) if (funcResult instanceof LambdaStopStatus result) {
                                return result.value;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }
    public static Object blockIterator(Vec3d pos, int range, BiFunction<BlockPos, BlockState, LambdaStopStatus> func) {
        return blockIterator(BlockPos.ofFloored(pos), range, func);
    }

    public static final float CRYSTAL_POWER = 12f;
    public static final float BED_POWER = 10f;
    public static final float ANCHOR_POWER = 10f;

    public static Vec3d findSafeExplosion(LivingEntity entity, Vec3d targetPos, Vec3d explosionPos, float power, int range, double maxDamage) {
        Box eBox = entity.getBoundingBox();

        if (DamageUtils.explosionDamage(entity, targetPos, eBox, explosionPos, power, DamageUtils.HIT_FACTORY) <= maxDamage && TPUtils.isPlayerSafePos(BlockPos.ofFloored(targetPos))) return targetPos;

        AtomicReference<Vec3d> result = new AtomicReference<>(targetPos);

        Vec3d iterResult = (Vec3d) blockIterator(targetPos, range, (bp, bs) -> {
            if (TPUtils.isPlayerSafePos(bp, bs)) {
                Vec3d pos = bp.toCenterPos().subtract(0,0.5,0);
                Box posBox = eBox.offset(pos.subtract(entity.getPos()));
                Box resultBox = eBox.offset(result.get().subtract(entity.getPos()));
                float damage = DamageUtils.explosionDamage(entity, pos, posBox, explosionPos, power, DamageUtils.HIT_FACTORY);
                float resultDamage = DamageUtils.explosionDamage(entity, result.get(), resultBox, explosionPos, power, DamageUtils.HIT_FACTORY);

                if (damage <= maxDamage) return new LambdaStopStatus(pos);
                if (resultDamage <= maxDamage) return new LambdaStopStatus(result.get());
                if (damage < resultDamage) result.set(pos);
            }

            return null;
        });

        if (iterResult == null) return result.get();
        else return iterResult;
    }
    public static Vec3d findSafeExplosion(LivingEntity entity, Vec3d explosionPos, float power, int range, double maxDamage) {
        return findSafeExplosion(entity, entity.getPos(), explosionPos, power, range, maxDamage);
    }

    public static Vec3d findMaxExplosion(LivingEntity entity, Vec3d targetPos, Vec3d explosionPos, float power, int range, double minDamage) {
        Box eBox = entity.getBoundingBox();

        if (DamageUtils.explosionDamage(entity, targetPos, eBox, explosionPos, power, DamageUtils.HIT_FACTORY) >= minDamage && TPUtils.isPlayerSafePos(BlockPos.ofFloored(targetPos))) return targetPos;

        AtomicReference<Vec3d> result = new AtomicReference<>(targetPos);

        Vec3d iterResult = (Vec3d) blockIterator(targetPos, range, (bp, bs) -> {
            if (TPUtils.isPlayerSafePos(bp, bs)) {
                Vec3d pos = bp.toCenterPos().subtract(0,0.5,0);
                Box posBox = eBox.offset(pos.subtract(entity.getPos()));
                Box resultBox = eBox.offset(result.get().subtract(entity.getPos()));
                float damage = DamageUtils.explosionDamage(entity, pos, posBox, explosionPos, power, DamageUtils.HIT_FACTORY);
                float resultDamage = DamageUtils.explosionDamage(entity, result.get(), resultBox, explosionPos, power, DamageUtils.HIT_FACTORY);

                if (damage >= minDamage) return new LambdaStopStatus(pos);
                if (resultDamage >= minDamage) return new LambdaStopStatus(result.get());
                if (damage > resultDamage) result.set(pos);
            }

            return null;
        });

        if (iterResult == null) return result.get();
        else return iterResult;
    }
    public static Vec3d findMaxExplosion(LivingEntity entity, Vec3d explosionPos, float power, int range, double minDamage) {
        return findSafeExplosion(entity, entity.getPos(), explosionPos, power, range, minDamage);
    }

    public static Entity findTarget(Set<EntityType<?>> entities) {
        return TargetUtils.get(entity -> {
            if (entity == mc.player || entity == mc.cameraEntity) return false;
            if ((entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) || !entity.isAlive()) return false;
            if (entity instanceof PlayerEntity) {
                if (!Friends.get().shouldAttack((PlayerEntity) entity)) return false;
            }
            if (!entities.contains(entity.getType())) return false;
            return true;
        }, SortPriority.LowestDistance);
    }
    public static boolean canPlaceCrystal(BlockPos obsidian, BlockState state) {
        if (mc.world == null) return false;
        if (obsidian == null || state == null) return false;
        if (!World.isValid(obsidian)) return false;
        BlockPos up = obsidian.up();
        Box upBox = new Box(up);
        BlockState upState = mc.world.getBlockState(up);
        return state.getBlock() == Blocks.OBSIDIAN &&
            upState.isAir() &&
            mc.world.getEntitiesByClass(
                Entity.class,
                upBox,
                e -> true
            ).isEmpty();
    }
    public static boolean canPlaceCrystal(BlockPos obsidian) {
        if (mc.world == null) return false;
        return canPlaceCrystal(obsidian, mc.world.getBlockState(obsidian));
    }
    public static boolean canPlaceBlock(BlockPos pos, BlockState state, Block block) {
        if (mc.world == null) return false;
        if (pos == null || state == null) return false;
        if (!World.isValid(pos)) return false;
        return mc.world.canPlace(block.getDefaultState(), pos, ShapeContext.absent());
    }
}
