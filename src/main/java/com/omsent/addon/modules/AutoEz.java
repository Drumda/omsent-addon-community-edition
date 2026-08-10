package com.omsent.addon.modules;

import com.omsent.addon.NModule;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AutoEz extends NModule {
    private static final AutoEz INSTANCE = new AutoEz();
    public static AutoEz getInstance() { return INSTANCE; }

    private final SettingGroup sgGeneral = settings.createGroup("General");

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("Range to detect player death (blocks, 0-50)")
        .defaultValue(20)
        .min(0)
        .max(50)
        .sliderRange(0, 50)
        .build()
    );

    private final Setting<String> customText = sgGeneral.add(new StringSetting.Builder()
        .name("custom-text")
        .description("Custom text sent after the player's name")
        .defaultValue("ez")
        .build()
    );

    public AutoEz() {
        super("autoez", "Detects player death packets within range and sends a custom message");
    }

    @Override
    public void onActivate() {
        if (!Main.enable) {
            toggle();
            return;
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket packet)) return;
        if (packet.getStatus() != 3) return;

        Entity entity = packet.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity player)) return;
        if (!player.isDead()) return;

        double distSq = mc.player.squaredDistanceTo(entity);
        if (distSq > (double) range.get() * range.get()) return;

        msg(EntityUtils.getName(player) + " " + customText.get());
    }
}
