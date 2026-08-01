package com.omsent.addon.modules;

import com.omsent.addon.NModule;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.compiler.Expr;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;

public class VclipPlus extends NModule {
    private static final VclipPlus INSTANCE = new VclipPlus();
    public static VclipPlus getInstance() { return INSTANCE; }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
        .name("distance")
        .description("Distance to clip upwards")
        .defaultValue(3.0)
        .min(0.1)
        .max(100.0)
        .sliderRange(0.1, 20.0)
        .build()
    );

    private final Setting<Integer> packets = sgGeneral.add(new IntSetting.Builder()
        .name("packets")
        .description("Number of packets to send")
        .defaultValue(20)
        .min(1)
        .max(100)
        .sliderRange(1, 50)
        .build()
    );

    private final Setting<Boolean> disableFlying = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-flying")
        .description("Disable flying after clipping")
        .defaultValue(true)
        .build()
    );

    // Render settings
    private final Setting<Boolean> render = sgGeneral.add(new BoolSetting.Builder()
        .name("render")
        .description("Render the clipping path")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> boxColor = sgGeneral.add(new ColorSetting.Builder()
        .name("box-color")
        .description("Color of the box outline")
        .defaultValue(new Color(255, 0, 0, 255))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> fillColor = sgGeneral.add(new ColorSetting.Builder()
        .name("fill-color")
        .description("Color of the box fill")
        .defaultValue(new Color(255, 0, 0, 50))
        .visible(render::get)
        .build()
    );

    private final Setting<Boolean> ForceClip = sgGeneral.add(new BoolSetting.Builder()
        .name("force")
        .description("Force the clipping")
        .defaultValue(true)
        .build()
    );

    private Vec3d currentPos;
    private double targetY;
    @Override
    public void onActivate() {
        if (!Main.enable) {
            toggle();
            return;
        }
    }
    public VclipPlus() {
        super("Vclip-Plus", "Allows upward clipping through walls by sending movement packets");
    };
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Main.enable) {
            toggle();
            return;
        }
        boolean canFly;
        if (mc.player == null) return;
        canFly = mc.player.isFallFlying();
        // Ensure player is in creative mode or has elytra equipped
        if (ForceClip.get() == false) {
            if (!canFly) {
                error("You need to have elytra equipped to use Vclip");
                toggle();
                return;
            }
        }
        // Start clipping
        clipUpwards();

        // Disable the module after clipping
        toggle();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || mc.player == null || currentPos == null) return;

        // Calculate the box for the clipping path
        double width = 0.6; // Player width
        Box box = new Box(
            currentPos.x - width / 2, currentPos.y,
            currentPos.z - width / 2, currentPos.x + width / 2, targetY,
            currentPos.z + width / 2
        );

        // Render the box
        event.renderer.box(box, fillColor.get(), boxColor.get(), ShapeMode.Both, 0);
    }

    private void clipUpwards() {
        if (mc.player == null) return;

        currentPos = mc.player.getPos();
        double totalDistance = distance.get();
        int packetCount = packets.get();
        targetY = currentPos.y + totalDistance;

        // Calculate distance per packet
        double distancePerPacket = totalDistance / packetCount;

        // Send movement packets
        for (int i = 0; i < packetCount; i++) {
            // Calculate new position for this packet
            double newY = currentPos.y + (distancePerPacket * (i + 1));
            Vec3d newPos = new Vec3d(currentPos.x, newY, currentPos.z);

            // Send position packet
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                newPos.x,
                newPos.y,
                newPos.z,
                mc.player.isOnGround()
            ));
        }

        // Update client-side position
        mc.player.setPos(currentPos.x, targetY, currentPos.z);

        // Disable flying if enabled
        if (disableFlying.get()) {
            mc.player.getAbilities().flying = false;
        }
    }
}
