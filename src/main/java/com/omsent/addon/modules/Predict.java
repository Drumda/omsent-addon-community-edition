package com.omsent.addon.modules;

import com.omsent.addon.NModule;
import meteordevelopment.meteorclient.settings.*;

public class Predict extends NModule {
    private static final Predict INSTANCE = new Predict();
    public static Predict getInstance() { return INSTANCE; }

    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Predict mode")
            .defaultValue(Mode.Time)
            .build()
    );
    public final Setting<Integer> limit = sgGeneral.add(new IntSetting.Builder()
            .name("limit")
            .description("Limit max predict points")
            .defaultValue(2)
            .min(2)
            .sliderMax(5)
            .visible(() -> mode.get() == Mode.Time)
            .build()
    );
    public final Setting<Double> power = sgGeneral.add(new DoubleSetting.Builder()
            .name("power")
            .description("Result * power")
            .defaultValue(5)
            .min(0)
            .sliderRange(0.1, 10)
            .build()
    );

    public Predict() {
        super("Predict", "Predict player move by omsent & osmuikosmh");
    }

    public static enum Mode {
        Time,
        Velocity
    }
}
