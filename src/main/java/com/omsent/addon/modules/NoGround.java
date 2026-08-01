package com.omsent.addon.modules;

import com.omsent.addon.AddonTemplate;
import com.omsent.addon.Utils.ChatUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.text.*;
import org.spongepowered.asm.mixin.Mutable;


public class NoGround extends Module {
    private static final NoGround INSTANCE = new NoGround();
    public static NoGround getInstance() { return INSTANCE; }
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Double> yVelocity = sgGeneral.add(new DoubleSetting.Builder()
        .name("Ground Y Velocity")
        .defaultValue(0.1)
        .min(0.1)
        .max(1.0)
        .sliderRange(0.1, 1.0)
        .build()
    );
    public NoGround() {
        super (AddonTemplate.CATEGORY,"No-Ground","no fall damage By omsent");
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (!Main.enable) {
            toggle();
            return;
        }
    }

    @EventHandler
    private void onPacketSeed(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket packet) {
        ((PlayerMoveC2SPacketAccessor) packet).setOnGround(false);
        }
    }
    @Override
    public void onActivate() {
        if (!Main.enable) {
            toggle();
            return;
        }
    }
    @Override
    public void onDeactivate() {
        if (mc == null) return;
        if (mc.player != null) {
            mc.player.setVelocity(0, yVelocity.get(), 0);
        }
    }
    @Override
    public void sendToggledMsg() {
        if (Config.get().chatFeedback.get() && chatFeedback) {
            MutableText onMsg = Text.empty().setStyle(Style.EMPTY.withColor(Color.fromRGBA(0,255,37,255))).append("Enable");
            MutableText offMsg = Text.empty().setStyle(Style.EMPTY.withColor(Color.fromRGBA(255,0,255,255))).append("Disable");
            MutableText toggleMsg = Text.empty();
            toggleMsg.append(Text.empty().setStyle(Style.EMPTY.withColor(Formatting.WHITE)).append(this.title));
            toggleMsg.append(" ");
            toggleMsg.append(this.isActive() ? onMsg : offMsg);
            meteordevelopment.meteorclient.utils.player.ChatUtils.forceNextPrefixClass(getClass());
            //Output feed back
            ChatUtils.sendMsg(toggleMsg, this.name);
        }
    }
}
