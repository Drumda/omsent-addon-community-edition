package com.omsent.addon.modules;

import com.omsent.addon.GliemtUtils.GEncryptUtils;
import com.omsent.addon.GliemtUtils.StringUtils;
import com.omsent.addon.NModule;
import com.omsent.addon.Utils.ChatUtils;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;

public class ChatEncrypt extends NModule {
    public ChatEncrypt() {
        super("Chat Encrypt", "?");
    }

    private final String key = "//OhfIVoGXX0hMed20YJ6mSltujbii9CPwaRwRa70X0=";
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    boolean receive;

    @EventHandler
    private void onSendMessage(SendMessageEvent event) { //
        if (mc.player == null || mc.world == null) return;
        String encryptMessage = "";
        encryptMessage = GEncryptUtils.encrypt("(" + mc.player.getName().getString() + ") " + event.message, key);
        event.message = "$#!" + encryptMessage;
    }
    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (mc.world == null || mc.player == null) return;

        if (receive) return;

        String receiveMessage = event.getMessage().getString();
        String encryptMessage = "";
        String decryptMessage = "";
        if (receiveMessage.contains("$#!")) {
            encryptMessage = StringUtils.getAfter(receiveMessage, "$#!");
            decryptMessage = GEncryptUtils.decrypt(encryptMessage, key);
            receive = true;
            ChatUtils.sendMsg("Decrypt message: " + decryptMessage);
            receive = false;
        }
    }

}
