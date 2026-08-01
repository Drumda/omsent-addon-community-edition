package com.omsent.addon.Utils;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.entity.Target;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;

public class EntityUtils {
    private EntityUtils() {}

    public static void attack(Entity entity, boolean rotation, boolean swing) {
        if (!TPUtils.allowSendP()) return;
        if (rotation) Rotations.rotate(Rotations.getYaw(entity), Rotations.getPitch(entity, Target.Body));
        if (swing) mc.player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
    }
    public static void attack(Entity entity) {
        attack(entity, false, false);
    }

    public static boolean isAttack(PlayerInteractEntityC2SPacket packet) {
        return ((Enum) ((IPlayerInteractEntityC2SPacket) packet).getType()).name() == "ATTACK";
    }
    public static Entity getEntity(PlayerInteractEntityC2SPacket packet) {
        return ((IPlayerInteractEntityC2SPacket) packet).getEntity();
    }
}
