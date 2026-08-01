package com.omsent.addon.GliemtUtils;

import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class PacketUtils {
    private PacketUtils() {
        //私有构造器
    }
    public static void disconnect(String reason) {
        if (mc.player == null || mc.player.networkHandler == null) return;
        mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal(reason)));
    }
}
