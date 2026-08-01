package com.omsent.addon.modules;

import com.omsent.addon.NModule;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;

public class AutoPot extends NModule {
    private static final AutoPot INSTANCE = new AutoPot();
    public static AutoPot getInstance() { return INSTANCE; }

    private final SettingGroup sgGeneral = settings.createGroup("General");

    private final Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder()
        .name("keybind")
        .description("Key to throw turtle potion")
        .defaultValue(Keybind.fromKey(GLFW_KEY_UNKNOWN))
        .build()
    );

    private final Setting<Boolean> silentSwap = sgGeneral.add(new BoolSetting.Builder()
        .name("silent-swap")
        .description("Silently swap to potion")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotate to throw potion")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-ground")
        .description("Only throw when on ground")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Cooldown between throws (ticks)")
        .defaultValue(10)
        .min(0)
        .max(100)
        .sliderRange(0, 50)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Swap back to previous item")
        .defaultValue(true)
        .build()
    );

    private int lastThrowTick = 0;
    private int previousSlot = -1;

    public AutoPot() {
        super("AutoPot", "Automatically throw turtle potion on key press");
    }
    @Override
    public void onActivate() {
        if (!Main.enable) {
            toggle();
            return;
        }
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Main.enable) {
            toggle();
            return;
        }
        if (mc.player == null || mc.world == null) return;

        if (keybind.get() == null || !keybind.get().isPressed()) return;

        if (onlyGround.get() && !mc.player.isOnGround()) {
            return;
        }

        if (mc.player.age - lastThrowTick < cooldown.get()) {
            return;
        }

        throwTurtlePotion();
    }

    private void throwTurtlePotion() {
        FindItemResult result = findTurtlePotion();

        if (!result.found()) {
            error("No turtle potion found in inventory!");
            return;
        }

        if (result.slot() < 9) {
            throwPotionFromHotbar(result.slot());
        } else {
            throwPotionFromInventory(result.slot());
        }

        if (mc.player != null) {
            lastThrowTick = mc.player.age;
        }
    }

    private FindItemResult findTurtlePotion() {
        if (mc.player != null) {
            for (int i = 0; i < mc.player.getInventory().size(); i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.getItem() == Items.SPLASH_POTION || stack.getItem() == Items.LINGERING_POTION) {
                    if (isTurtlePotion(stack)) {
                        return new FindItemResult(i, stack.getCount());
                    }
                }
            }
        }
        return new FindItemResult(0, 0);
    }

    private boolean isTurtlePotion(ItemStack stack) {
        try {
            PotionContentsComponent potion = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (potion == null) return false;

            return potion.potion().stream().anyMatch(effect -> {
                String effectName = effect.getType().name().toLowerCase();
                return effectName.contains("turtle") || effectName.contains("resistance");
            });
        } catch (Exception e) {
            return false;
        }
    }

    private void throwPotionFromHotbar(int slot) {
        if (mc.player != null) {
            previousSlot = mc.player.getInventory().selectedSlot;
        }
        float originalYaw = 0;
        if (mc.player != null) {
            originalYaw = mc.player.getYaw();
        }
        float originalPitch = mc.player.getPitch();

        if (silentSwap.get()) {
            InvUtils.swap(slot, true);
        } else {
            mc.player.getInventory().selectedSlot = slot;
        }

        if (rotate.get()) {
            mc.player.setYaw(originalYaw);
            mc.player.setPitch(90);
        }

        mc.options.useKey.setPressed(true);
        if (mc.interactionManager != null) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }
        mc.options.useKey.setPressed(false);

        if (rotate.get()) {
            mc.player.setYaw(originalYaw);
            mc.player.setPitch(originalPitch);
        }

        if (swapBack.get() && previousSlot >= 0) {
            if (silentSwap.get()) {
                InvUtils.swap(previousSlot, true);
            } else {
                mc.player.getInventory().selectedSlot = previousSlot;
            }
        }
    }

    private void throwPotionFromInventory(int slot) {
        if (!silentSwap.get()) {
            error("Silent swap is required for inventory items!");
            return;
        }

        previousSlot = mc.player.getInventory().selectedSlot;
        float originalYaw = mc.player.getYaw();
        float originalPitch = mc.player.getPitch();
        InvUtils.swap(slot, true);

        if (rotate.get()) {
            mc.player.setYaw(originalYaw);
            mc.player.setPitch(90);
        }

        mc.options.useKey.setPressed(true);
        if (mc.interactionManager != null) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }
        mc.options.useKey.setPressed(false);

        if (rotate.get()) {
            mc.player.setYaw(originalYaw);
            mc.player.setPitch(originalPitch);
        }

        if (swapBack.get() && previousSlot >= 0) {
            InvUtils.swap(previousSlot, true);
        }
    }
}
