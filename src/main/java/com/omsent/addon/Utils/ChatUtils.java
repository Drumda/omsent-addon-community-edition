package com.omsent.addon.Utils;

import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.Objects;

public class ChatUtils {
    private ChatUtils() {}
    public static int getHash(String s) {
        if (Config.get().deleteChatFeedback.get())
            return Objects.hash(s);
        else return 0;
    }
    public static Text getPrefix() {
        String text = "omsent";
        int[] startCol = new int[]{100,130,152};
        int[] endCol = new int[]{99,151,127};
        MutableText prefix = Text.empty().append("[");
        for (int i = 0; i < text.length(); ++i) {
            float ratio = (float)i / (float)(text.length() - 1);
            int red = (int)(startCol[0] + ratio * (endCol[0] - startCol[0]));
            int green = (int)(startCol[1] + ratio * (endCol[1] - startCol[1]));
            int blue = (int)(startCol[2] + ratio * (endCol[2] - startCol[2]));

            prefix.append(Text.literal(String.valueOf(text.charAt(i)))
            .setStyle(Style.EMPTY.withColor(Color.fromRGBA(red, green, blue, 255))));
        }
        return prefix.append("] ");
    }

    public static void sendClientMsg(Text msg, int id) {
        ((IChatHud)MeteorClient.mc.inGameHud.getChatHud()).meteor$add(msg, id);
    }
    public static void sendClientMsg(String msg, int id) {
        sendClientMsg(Text.literal(msg), id);
    }

    public static void sendMsg(Text msg, int id) {
        if (MeteorClient.mc.world == null) return;
        if (msg == null) return;

        MutableText message = Text.empty()
            .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
            .append(getPrefix())
            .append(msg);

        sendClientMsg(message, id);
    }
    public static void sendMsg(Text msg) {
        sendMsg(msg, 0);
    }
    public static void sendMsg(String msg) {
        sendMsg(msg, 0);
    }
    public static <T> void sendMsg(T msg) {
        sendMsg(msg, 0);
    }
    public static void sendMsg(String msg, int id) {
        sendMsg(Text.literal(msg), id);
    }
    public static <T> void sendMsg(T msg, int id) {
        sendMsg(String.valueOf(msg), id);
    }
    public static void sendMsg(Text msg, String name) {
        sendMsg(msg, getHash(name));
    }

    public static void sendMsg(String msg, String name) {
        sendMsg(Text.literal(msg), getHash(name));
    }
    public static <T> void sendMsg(T msg, String name) {
        sendMsg(String.valueOf(msg), getHash(name));
    }
}
