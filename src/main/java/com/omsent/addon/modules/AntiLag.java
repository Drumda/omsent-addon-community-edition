package com.omsent.addon.modules;

import com.omsent.addon.AddonTemplate;
import com.omsent.addon.NModule;
import com.omsent.addon.Utils.TPUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;
import meteordevelopment.meteorclient.systems.modules.Module;

public class AntiLag extends NModule {
    private static final AntiLag INSTANCE = new AntiLag();
    public static AntiLag getInstance() { return INSTANCE; }

    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRubberband = this.settings.createGroup("Anti Rubberband");

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
            .name("debug")
            .description("Print debug information")
            .defaultValue(false)
            .build()
    );
    public final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Max range")
            .defaultValue(200.0)
            .min(0.0)
            .sliderRange(0.0, 256.0)
            .build()
    );
    public final Setting<Double> moveD = sgGeneral.add(new DoubleSetting.Builder()
            .name("move-distance")
            .description("Move distance")
            .defaultValue(10.0)
            .sliderRange(0, 128.0)
            .build()
    );

    private final Setting<Boolean> antiRubberband = sgRubberband.add(new BoolSetting.Builder()
            .name("anti-rubberband")
            .description("Anti rubberband when server pulls you back")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> rubberbandHeight = sgRubberband.add(new DoubleSetting.Builder()
            .name("rubberband-height")
            .description("Height to teleport up/down when anti rubberband")
            .defaultValue(200.0)
            .min(50.0)
            .max(500.0)
            .sliderRange(50.0, 500.0)
            .visible(antiRubberband::get)
            .build()
    );

    private final Setting<Double> rubberbandThreshold = sgRubberband.add(new DoubleSetting.Builder()
            .name("rubberband-threshold")
            .description("Minimum distance to consider as rubberband")
            .defaultValue(0.5)
            .min(0.1)
            .max(10.0)
            .sliderRange(0.1, 10.0)
            .visible(antiRubberband::get)
            .build()
    );

    private final Setting<Integer> rubberbandPackets = sgRubberband.add(new IntSetting.Builder()
            .name("rubberband-packets")
            .description("Number of packets to send when anti rubberband")
            .defaultValue(20)
            .min(1)
            .max(100)
            .sliderRange(1, 50)
            .visible(antiRubberband::get)
            .build()
    );

    private final Setting<Boolean> cancelRubberband = sgRubberband.add(new BoolSetting.Builder()
            .name("cancel-rubberband")
            .description("Cancel the rubberband packet from server")
            .defaultValue(true)
            .visible(antiRubberband::get)
            .build()
    );

    private Vec3d lastValidPosition;
    private boolean isRubberbanding = false;
    private int rubberbandCooldown = 0;

    public AntiLag() {
        super("AntiLag", "Anti server lag your body and head.");
    }

    @Override
    public void onActivate() {
        if (!Main.enable) {
            toggle();
            return;
        }
        if (mc.player != null) {
            lastValidPosition = mc.player.getPos();
        }
        isRubberbanding = false;
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) return;

        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            PlayerPositionLookS2CPacket packet = (PlayerPositionLookS2CPacket) event.packet;
            Vec3d serverPos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
            Vec3d currentPos = mc.player.getPos();
            double distance = currentPos.distanceTo(serverPos);

            if (debug.get()) {
                msg("Server pos: " + serverPos + ", Current pos: " + currentPos + ", Distance: " + distance);
            }

            if (antiRubberband.get() && distance > rubberbandThreshold.get() && !isRubberbanding && rubberbandCooldown == 0) {
                if (lastValidPosition != null) {
                    if (cancelRubberband.get()) {
                        event.cancel();
                    }
                    handleRubberband(lastValidPosition);
                    isRubberbanding = true;
                    rubberbandCooldown = 20;
                }
            }
        }
    }

    private void handleRubberband(Vec3d targetPos) {
        if (mc.player == null) return;

        Vec3d currentPos = mc.player.getPos();
        double height = rubberbandHeight.get();

        Vec3d upPos = new Vec3d(currentPos.x, currentPos.y + height, currentPos.z);
        Vec3d targetUpPos = new Vec3d(targetPos.x, currentPos.y + height, targetPos.z);
        Vec3d downPos = new Vec3d(targetPos.x, targetPos.y, targetPos.z);

        if (debug.get()) {
            msg("Anti Rubberband triggered!");
            msg("Target position: " + targetPos);
        }

        int packetCount = rubberbandPackets.get();

        for (int i = 0; i < packetCount; i++) {
            mc.player.setPos(upPos.x, upPos.y, upPos.z);
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(upPos.x, upPos.y, upPos.z, false));
        }

        for (int i = 0; i < packetCount; i++) {
            mc.player.setPos(targetUpPos.x, targetUpPos.y, targetUpPos.z);
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(targetUpPos.x, targetUpPos.y, targetUpPos.z, false));
        }

        for (int i = 0; i < packetCount; i++) {
            mc.player.setPos(downPos.x, downPos.y, downPos.z);
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(downPos.x, downPos.y, downPos.z, false));
        }

        lastValidPosition = downPos;
        isRubberbanding = false;

        if (debug.get()) {
            msg("Anti Rubberband completed! Sent " + (packetCount * 3) + " packets");
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null) return;

        if (event.packet instanceof TeleportConfirmC2SPacket) {
            lastValidPosition = mc.player.getPos();
            isRubberbanding = false;
        }
    }

    @EventHandler
    private void onTick(meteordevelopment.meteorclient.events.world.TickEvent.Post event) {
        if (rubberbandCooldown > 0) {
            rubberbandCooldown--;
        }

        if (mc.player != null) {
            lastValidPosition = mc.player.getPos();
        }
    }
}
