package com.omsent.addon.modules;

import com.omsent.addon.NModule;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Random;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AutoEz extends NModule {
    private static final AutoEz INSTANCE = new AutoEz();
    public static _34782758972479_String = "欢迎各位 skider 前来skid乐乐乐乐乐";
    public static AutoEz getInstance() { return INSTANCE; }

    private static final String RANDOM_CHARS = "123456789abcdefghijklmnopqrstuvwxyz";
    private static final Random random = new Random();

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

    private final Setting<String> selfDeathText = sgGeneral.add(new StringSetting.Builder()
        .name("self-death-text")
        .description("Text sent when you die")
        .defaultValue("gg")
        .build()
    );

    private final Setting<String> otherDeathText = sgGeneral.add(new StringSetting.Builder()
        .name("other-death-text")
        .description("Text sent when another player dies in range")
        .defaultValue("ez")
        .build()
    );

    private final Setting<Integer> randomLength = sgGeneral.add(new IntSetting.Builder()
        .name("random-length")
        .description("Length of random suffix appended to other death text (0 = disabled)")
        .defaultValue(0)
        .min(0)
        .max(20)
        .sliderRange(0, 20)
        .build()
    );

    public AutoEz() {
        super("autoez", "Detects player death and sends custom text");
    }

    private static String generateRandom(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM_CHARS.charAt(random.nextInt(RANDOM_CHARS.length())));
        }
        return sb.toString();
    }

    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (!(event.entity instanceof PlayerEntity player)) return;

        if (player == mc.player) {
            mc.getNetworkHandler().sendChatMessage(selfDeathText.get());
            return;
        }

        if (!player.isDead()) return;

        double distSq = mc.player.squaredDistanceTo(player);
        if (distSq > (double) range.get() * range.get()) return;

        String name = EntityUtils.getName(player);
        if (name == null) return;

        String suffix = randomLength.get() > 0 ? " " + generateRandom(randomLength.get()) : "";
        mc.getNetworkHandler().sendChatMessage(name + " " + otherDeathText.get() + suffix);
    }
}
