package com.omsent.addon.modules;

import com.omsent.addon.NModule;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;

public class SetVel_qwq extends NModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public SetVel_qwq() {
        super("QAQ_SetVel", "Wow!");
    }
    private final Setting<Boolean> StaticVel = sgGeneral.add(new BoolSetting.Builder()
        .name("Static vel")
        .description("?")
        .defaultValue(false)
        .build()
    );
    private final Setting<Integer> VelX = sgGeneral.add(new IntSetting.Builder()
        .name("Vel x")
        .description("")
        .defaultValue(0)
        .sliderRange(0, 25565)
        .build()
    );
    private final Setting<Integer> VelY = sgGeneral.add(new IntSetting.Builder()
        .name("Vel y")
        .description("")
        .defaultValue(0)
        .sliderRange(0, 25565)
        .build()
    );
    private final Setting<Integer> VelZ = sgGeneral.add(new IntSetting.Builder()
        .name("Vel z")
        .description("")
        .defaultValue(0)
        .sliderRange(0, 25565)
        .build()
    );
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Main.enable) {
            toggle();
            return;
        }
        if (StaticVel.get() == false) return;

        if (mc.player == null) {
            return;
        }
        mc.player.setVelocity(VelX.get(), VelY.get(), VelZ.get());
    }
    @Override
    public void onActivate() { //当打开功能
            if (!Main.enable) {
                toggle();
                return;
            }
        if (StaticVel.get() == true) return;
        if (mc.player == null) {
            toggle();
            return;
        }
        mc.player.setVelocity(VelX.get(), VelY.get(), VelZ.get());
        toggle();
    }
}
