package com.omsent.addon.modules;

import com.omsent.addon.GliemtUtils.DebugUtils;
import com.omsent.addon.GliemtUtils.PacketUtils;
import com.omsent.addon.NModule;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import org.apache.commons.lang3.ObjectUtils;

import java.io.IOException;
import java.util.Objects;

import static java.lang.System.exit;

public final class Main extends NModule {
    public Main() {
        super("Main", "You must enable this module only if you use other feature!");
    }
    public static boolean enable = false;

    @Override
    public void onActivate() {
        enable = true;
    }
    @Override
    public void onDeactivate() {
        enable = false;
    }
    
}
