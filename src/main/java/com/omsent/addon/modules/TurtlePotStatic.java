package com.omsent.addon.modules;

import com.omsent.addon.NModule;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

public class TurtlePotStatic extends NModule {
    private static final TurtlePotStatic INSTANCE = new TurtlePotStatic();
    public static TurtlePotStatic getInstance() { return INSTANCE; }

    private final SettingGroup sgGeneral = settings.createGroup("General");

    private final Setting<Integer> targetSlot = sgGeneral.add(new IntSetting.Builder()
        .name("target-slot")
        .description("Target hotbar slot (0-8)")
        .defaultValue(0)
        .min(0)
        .max(8)
        .sliderRange(0, 8)
        .build()
    );

    private final Setting<Integer> checkInterval = sgGeneral.add(new IntSetting.Builder()
        .name("check-interval")
        .description("Check interval in ticks")
        .defaultValue(5)
        .min(1)
        .max(20)
        .sliderRange(1, 20)
        .build()
    );

    private int tickCounter = 0;

    @Override
    public void onActivate() {
        if (!Main.enable) {
            toggle();
            return;
        }
    }
    public TurtlePotStatic() {
        super("TurtlePotStatic", "Move turtle potion from inventory to specified hotbar slot");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!Main.enable) {
            toggle();
            return;
        }
        if (mc.player == null || mc.world == null) return;

        tickCounter++;
        if (tickCounter < checkInterval.get()) return;
        tickCounter = 0;

        int target = targetSlot.get();

        // 只从背包中查找神龟药水
        for (int i = 9; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.SPLASH_POTION) {
                // 找到神龟药水，移动到目标槽位
                msg("找到神龟药水，从槽位 " + i + " 移动到槽位 " + target);
                InvUtils.swap(target, true);
                return;
            }
        }
        msg("未找到神龟药水");
    }

    private boolean isTurtlePotion(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try {
            PotionContentsComponent potion = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (potion == null) return false;

            // 调试信息：打印药水效果
            if (potion.getEffects() != null) {
                for (var effect : potion.getEffects()) {
                    if (mc.player == null) return false;
                    if(mc.player.hasStatusEffect(StatusEffects.RESISTANCE)) return true;
                }
            }
            return false;
        } catch (Exception e) {
            msg("检查药水时出错: " + e.getMessage());
            return false;
        }
    }
}
