package com.omsent.addon;

import com.omsent.addon.Utils.ChatUtils;
import com.omsent.addon.Utils.TPUtils;
import com.omsent.addon.NModule;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.util.Formatting;
import net.minecraft.text.*;
import net.minecraft.network.packet.Packet;

public abstract class NModule extends Module {
    public NModule(String name, String desc) {
        super(AddonTemplate.CATEGORY, name, desc);
    }

    @Override
    public void sendToggledMsg() {

        if (Config.get().chatFeedback.get() && chatFeedback) {
            MutableText onMsg = Text.empty().setStyle(Style.EMPTY.withColor(Color.fromRGBA(0,255,0,255))).append("Enable √");
            MutableText offMsg = Text.empty().setStyle(Style.EMPTY.withColor(Color.fromRGBA(255,0,0,255))).append("Disable ×");
            MutableText toggledMsg = Text.empty();
            toggledMsg.append(Text.empty().setStyle(Style.EMPTY.withColor(Formatting.WHITE)).append(this.title));
            toggledMsg.append(" ");
            toggledMsg.append(this.isActive() ? onMsg : offMsg);
            meteordevelopment.meteorclient.utils.player.ChatUtils.forceNextPrefixClass(getClass());

            ChatUtils.sendMsg(toggledMsg, this.name);
        }
    }

    public void clientMsg(Text message, int id) {
        ChatUtils.sendClientMsg(message, id);
    }
    public void clientMsg(String message, int id) {
        ChatUtils.sendClientMsg(message, id);
    }
    public void msg(Text message, int id) {
        MutableText t = Text.empty().append(
            Formatting.RESET+"["
            +Formatting.LIGHT_PURPLE
            +this.name
            +Formatting.RESET+"] "
        );
        t.append(message);
        ChatUtils.sendMsg(t, id);
    }
    public void error(String message) {
        MutableText t = Text.empty().append(
            Formatting.RESET+"["
            +Formatting.RED
            +"ERROR"
            +Formatting.RESET+"] "
        );
        t.append(message);
        ChatUtils.sendMsg(t, 0);
    }

    public void msg(String message, int id) {
        msg(Text.literal(message), id);
    }
    public <T> void msg(T message, int id) {
        msg(String.valueOf(message), id);
    }
    public void msg(Text message) {
        msg(message, 0);
    }
    public void msg(String message) {
        msg(Text.literal(message), 0);
    }
    public <T> void msg(T message) {
        msg(String.valueOf(message));
    }
    public void msg(Text message, String name) {
        msg(message, ChatUtils.getHash(name));
    }
    public void msg(String message, String name) {
        msg(Text.literal(message), name);
    }
    public <T> void msg(T message, String name) {
        msg(String.valueOf(message), name);
    }

    // Packet
    public void sendPacket(Packet<?> packet) {
        if (!TPUtils.allowSendP()) return;
        mc.getNetworkHandler().sendPacket(packet);
    }

}
