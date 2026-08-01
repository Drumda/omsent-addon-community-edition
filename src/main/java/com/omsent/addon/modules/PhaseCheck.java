package com.omsent.addon.modules;

import com.omsent.addon.NModule;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PhaseCheck extends NModule {
    private static final PhaseCheck INSTANCE = new PhaseCheck();
    public static PhaseCheck getInstance() { return INSTANCE; }

    public enum DisplayMode {
        Constant,
        Blink
    }

    private final SettingGroup sgGeneral = settings.createGroup("General");

    private final Setting<Boolean> enabled = sgGeneral.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Enable phase check")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> xOffset = sgGeneral.add(new DoubleSetting.Builder()
        .name("x-offset")
        .description("X offset for the text")
        .defaultValue(0.0)
        .min(-1000.0)
        .max(1000)
        .sliderRange(-1000.0, 1000)
        .visible(enabled::get)
        .build()
    );

    private final Setting<Double> yOffset = sgGeneral.add(new DoubleSetting.Builder()
        .name("y-offset")
        .description("Y offset for the text")
        .defaultValue(10.0)
        .min(-1000.0)
        .max(1000)
        .sliderRange(-1000.0, 1000)
        .visible(enabled::get)
        .build()
    );

    private final Setting<Double> textSize = sgGeneral.add(new DoubleSetting.Builder()
        .name("text-size")
        .description("Size of the text")
        .defaultValue(1.0)
        .min(0.5)
        .max(3.0)
        .sliderRange(0.5, 3.0)
        .visible(enabled::get)
        .build()
    );

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("text-color")
        .description("Color of the text")
        .defaultValue(new Color(0, 255, 0, 255))
        .visible(enabled::get)
        .build()
    );

    private final Setting<DisplayMode> displayMode = sgGeneral.add(new EnumSetting.Builder<DisplayMode>()
        .name("display-mode")
        .description("Display mode for text")
        .defaultValue(DisplayMode.Constant)
        .visible(enabled::get)
        .build()
    );

    private final Setting<Double> blinkSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("blink-speed")
        .description("Blinking speed for text")
        .defaultValue(1.0)
        .min(0.1)
        .max(5.0)
        .sliderRange(0.1, 5.0)
        .visible(() -> enabled.get() && displayMode.get() == DisplayMode.Blink)
        .build()
    );

    private final Setting<Boolean> textShadow = sgGeneral.add(new BoolSetting.Builder()
        .name("text-shadow")
        .description("Enable shadow for text")
        .defaultValue(true)
        .visible(enabled::get)
        .build()
    );

    public PhaseCheck() {
        super("PhaseCheck", "Checks if player is stuck in blocks");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!Main.enable) {
            toggle();
            return;
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.player == null || !enabled.get()) return;

        boolean isStuck = checkIfStuckInBlocks();

        if (!isStuck) {
            renderPhaseableText();
        }
    }
    @Override
    public void onActivate() {
        if (!Main.enable) {
            toggle();
            return;
        }
    }
    private boolean checkIfStuckInBlocks() {
        if (mc.player == null || mc.world == null) return false;

        Box playerBox = mc.player.getBoundingBox();
        World world = mc.world;

        int minX = (int) Math.floor(playerBox.minX);
        int minY = (int) Math.floor(playerBox.minY);
        int minZ = (int) Math.floor(playerBox.minZ);
        int maxX = (int) Math.floor(playerBox.maxX);
        int maxY = (int) Math.floor(playerBox.maxY);
        int maxZ = (int) Math.floor(playerBox.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);

                    if (!state.isAir() && state.isSolidBlock(world, pos)) {
                        Box blockBox = state.getCollisionShape(world, pos).getBoundingBox().offset(pos);
                        if (playerBox.intersects(blockBox)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private void renderPhaseableText() {
        String text = "Phaseable!";
        Color color = textColor.get();
        double size = textSize.get();
        double screenWidth = mc.getWindow().getScaledWidth();
        double screenCenterX = screenWidth / 2.0;
        double textWidth = TextRenderer.get().getWidth(text) / 2.0;
        double x = screenCenterX - textWidth + xOffset.get();
        double y = yOffset.get();

        TextRenderer.get().begin(size, false, textShadow.get());
        TextRenderer.get().render(text, x, y, color, textShadow.get());
        TextRenderer.get().end();
    }
}
