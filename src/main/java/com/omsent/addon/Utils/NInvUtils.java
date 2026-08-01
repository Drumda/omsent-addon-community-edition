package com.omsent.addon.Utils;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.utils.player.*;

public class NInvUtils {
    public static int preFrom = -1;
    public static int preTo = -1;

    private NInvUtils() {}

    public static int getSelectedSlot() {
        return mc.player.getInventory().selectedSlot;
    }

    /**
     * @param from from index
     * @param to to index
    */
    public static void swap(int from, int to, boolean back) {
        if (from == to) return;
        InvUtils.move().from(from).to(to);
        if (back) {
            preFrom = from;
            preTo = to;
        }
    }
    public static void swap(int from, boolean back) {
        swap(from, getSelectedSlot(), back);
    }
    public static void swapBack() {
        if (preFrom != -1 && preTo != -1) {
            swap(preFrom, preTo, false);
            preFrom = preTo = -1;
        }

    }
}
