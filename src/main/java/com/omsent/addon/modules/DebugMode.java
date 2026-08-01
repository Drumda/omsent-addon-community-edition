package com.omsent.addon.modules;

import com.omsent.addon.NModule;
import com.omsent.addon.GliemtUtils.DebugUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;

public class DebugMode extends NModule {
    private static final DebugMode INSTANCE = new DebugMode();
    public static DebugMode getInstance() { return INSTANCE; }

    private final SettingGroup sgGeneral = settings.createGroup("General");

    private final Setting<Boolean> notifyOnJoin = sgGeneral.add(new BoolSetting.Builder()
        .name("notify-on-join")
        .description("Notify when joining the world")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> checkPeriodically = sgGeneral.add(new BoolSetting.Builder()
        .name("check-periodically")
        .name("check-periodically")
        .description("Check debug mode periodically")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> checkInterval = sgGeneral.add(new IntSetting.Builder()
        .name("check-interval")
        .description("Check interval in ticks")
        .defaultValue(100)
        .min(20)
        .max(600)
        .sliderRange(20, 300)
        .visible(checkPeriodically::get)
        .build()
    );

    private boolean lastDebugStatus = false;
    private int checkTimer = 0;

    public DebugMode() {
        super("DebugMode", "Detects and notifies debug mode status");
    }

    @Override
    public void onActivate() {
        if (mc.player != null && notifyOnJoin.get()) {
            checkAndNotifyDebugMode();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        if (checkPeriodically.get()) {
            checkTimer++;
            if (checkTimer >= checkInterval.get()) {
                checkTimer = 0;
                checkAndNotifyDebugMode();
            }
        }
    }

    private void checkAndNotifyDebugMode() {
        boolean isDebug = DebugUtils.isDebugMode();

        if (isDebug && !lastDebugStatus) {
            msg("现在是调试模式");
            lastDebugStatus = true;
        }
    }

    public static boolean isDebugMode() {
        return DebugUtils.isDebugMode();
    }
}
