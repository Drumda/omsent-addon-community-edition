package com.omsent.addon.modules;

import com.omsent.addon.AddonTemplate;
import com.omsent.addon.NModule;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import meteordevelopment.meteorclient.systems.modules.Module;

public class NoLoadScreen extends NModule {
    private static final NoLoadScreen INSTANCE = new NoLoadScreen();
    public static NoLoadScreen getInstance() { return INSTANCE; }

    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Boolean> portalGodMode = sgGeneral.add(new BoolSetting.Builder()
            .name("portal-god-mode")
            .description("In nether portal on god mode.")
            .defaultValue(false)
            .build()
    );

    public NoLoadScreen() {
        super("NoLoadScreen", "Cancel downloading terrain screen By omsent & osmuikosmh");
    }

    private boolean godMode = false;

    @Override
    public void onActivate() {
            if (!Main.enable) {
                toggle();
                return;
            }
        this.godMode = false;
    }

    @EventHandler(priority = EventPriority.HIGHEST + 999)
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DownloadingTerrainScreen) {
            if (portalGodMode.get()) {
                msg("GodMode ON");
                this.godMode = true;
            }
            event.cancel();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof TeleportConfirmC2SPacket) {
            if (godMode && portalGodMode.get()) event.cancel();
        }
    }
}
