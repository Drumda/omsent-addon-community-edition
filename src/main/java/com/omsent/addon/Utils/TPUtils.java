package com.omsent.addon.Utils;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import com.omsent.addon.modules.AntiLag;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.BlockState;

public class TPUtils {
    private TPUtils() {}
    public static boolean allowSendP() {
        return mc != null && mc.getNetworkHandler() != null;
    }
    public static Double getMoveD() {
        Double moveD = AntiLag.getInstance().moveD.get();
        return moveD > 0 ? moveD : Double.MAX_VALUE;
    }
    public static Double getRange() {
        return AntiLag.getInstance().range.get();
    }
    public static void tp(Vec3d fv, Vec3d v, float yaw, float pitch) {
        double dis = fv.distanceTo(v);
        int steps = (int)Math.ceil(dis / getMoveD());
        if (steps > 20) {
            ChatUtils.sendMsg("Limit tp packet: " + steps + " packets");
        }

        for (int i = 1; i <= steps; ++i) {
            moveP();
        }
        moveP(v, yaw, pitch);
    }
    public static void tp(Vec3d fv, Vec3d v) {
        double dis = fv.distanceTo(v);
        int steps = (int)Math.ceil(dis / getMoveD());
        if (steps > 20) {
            ChatUtils.sendMsg("Limit tp packet: " + steps + " packets", "NumberVectorUtilsTPUtils");
        }

        for (int i = 1; i <= steps; ++i) {
            moveP();
        }
        moveP(v);
    }
    public static void doTp(Vec3d fv, Vec3d v, float yaw, float pitch) {
        double dis = fv.distanceTo(v);
        int steps = (int)Math.ceil(dis / getMoveD());
        if (steps > 20) {
            ChatUtils.sendMsg("Limit tp packet: " + steps + " packets");
        }

        for (int i = 1; i <= steps; ++i) {
            moveP(fv);
        }
        moveP(v, yaw, pitch);
    }
    public static void doTp(Vec3d fv, Vec3d v) {
        double dis = fv.distanceTo(v);
        int steps = (int)Math.ceil(dis / getMoveD());
        if (steps > 20) {
            ChatUtils.sendMsg("Limit tp packet: " + steps + " packets", "NumberVectorUtilsTPUtils");
        }

        for (int i = 1; i <= steps; ++i) {
            moveP(fv);
        }
        moveP(v);
    }
    public static void moveP(double x, double y, double z) {
        if (!allowSendP()) return;
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
            x, y, z,
            false
        ));
    }
    public static void moveP(float yaw, float pitch) {
        if (!allowSendP()) return;
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
            yaw, pitch,
            false
        ));
    }
    public static void moveP(double x, double y, double z, float yaw, float pitch) {
        if (!allowSendP()) return;
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
            x, y, z,
            yaw, pitch,
            false
        ));
    }
    public static void moveP() {
        if (!allowSendP()) return;
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(
            false
        ));
    }
    public static void moveP(Vec3d v) {
        moveP(v.x, v.y, v.z);
    }
    public static void moveP(Vec3d v, float yaw, float pitch) {
        moveP(v.x, v.y, v.z, yaw, pitch);
    }
    public static boolean isSafePos(BlockPos pos, BlockState state) {
        if (mc == null || mc.world == null) return true;
        Block block = state.getBlock();
        if (!mc.world.getWorldBorder().contains(pos)) return false;
        if (state.isAir() || block instanceof PlantBlock) return true;
        if (state.getFluidState().isEmpty()) return false;
        if (block == Blocks.COBWEB || block == Blocks.END_PORTAL) return true;
        if (state.isReplaceable()) return true;
        return state.getCollisionShape(mc.world, pos).isEmpty();
    }
    public static boolean isSafePos(BlockPos pos) {
        return isSafePos(pos, mc.world.getBlockState(pos));
    }
    public static boolean isPlayerSafePos(BlockPos pos, BlockState state) {
        return isSafePos(pos, state) && isSafePos(pos.add(0,1,0));
    }
    public static boolean isPlayerSafePos(BlockPos pos) {
        return isPlayerSafePos(pos, mc.world.getBlockState(pos));
    }

    public static void threeTp(Vec3d fv, Vec3d v, float yaw, float pitch, boolean tryAntiLag) {
        if (!allowSendP()) return;
        Double vclip = findVClipTo(BlockPos.ofFloored(fv), BlockPos.ofFloored(v));
        if (vclip == null) {
            tp(fv, v, yaw, pitch);
            return;
        }
        Vec3d vclipVec = fv.add(0,vclip - fv.y,0);
        Vec3d vUp = v.add(0, vclip - v.y, 0);

        if (tryAntiLag) {
            tp(fv, vclipVec);
            tp(vclipVec, vUp);
            tp(vclipVec, v, yaw, pitch);
        } else {
            moveP(vclipVec);
            moveP(vUp);
            moveP(v, yaw, pitch);
        }
    }
    public static void threeTp(Vec3d fv, Vec3d v, float yaw, float pitch) {
        threeTp(fv, v, yaw, pitch, true);
    }
    public static void threeTp(Vec3d fv, Vec3d v, boolean tryAntiLag) {
        if (mc.player == null) return;
        threeTp(fv, v, mc.player.getYaw(), mc.player.getPitch(), tryAntiLag);
    }
    public static void threeTp(Vec3d fv, Vec3d v) {
        if (mc.player == null) return;
        threeTp(fv, v, mc.player.getYaw(), mc.player.getPitch());
    }

    public static Double findVClipTo(BlockPos fv, BlockPos v) {
        if (mc.world == null) return null;
        for (int y = 0; y <= (Math.abs(mc.world.getBottomY()) + mc.world.getHeight()) / 2; y++) {
            BlockPos up = fv.add(0, y, 0);
            BlockPos down = fv.add(0, -y, 0);
            if (up.getY() < mc.world.getHeight()) if (isPlayerSafePos(up)) {
                if (canTpXZ(up, v)) return (double) up.getY();
            }
            if (down.getY() >= mc.world.getBottomY()) if (isPlayerSafePos(down)) {
                if (canTpXZ(down, v)) return (double) down.getY();
            }
        }

        return null;
    }
    public static boolean canTpXZ(BlockPos fv, BlockPos v) {
        v = new BlockPos(v.getX(), fv.getY(), v.getZ());
        return BlockPos.stream(
            fv,
            v
        ).allMatch(b -> isPlayerSafePos(b));
    }
}
