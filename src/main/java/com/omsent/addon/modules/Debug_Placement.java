package com.omsent.addon.modules;

import com.omsent.addon.NModule;
import com.omsent.addon.Utils.NInvUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

public class Debug_Placement extends NModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> XOffset = sgGeneral.add(new IntSetting.Builder()
        .name("Place-X-Offset")
        .description("The range to search for targets")
        .defaultValue(5)
        .sliderRange(1,6)
        .build()
    );
    private final Setting<Integer> YOffset = sgGeneral.add(new IntSetting.Builder()
        .name("Place-Y-Offset")
        .description("The range to search for targets")
        .defaultValue(5)
        .sliderRange(1,6)
        .build()
    );
    private final Setting<Integer> ZOffset = sgGeneral.add(new IntSetting.Builder()
        .name("Place-Z-Offset")
        .description("The range to search for targets")
        .defaultValue(5)
        .sliderRange(1,6)
        .build()
    );
    private final Setting<Integer> PlaceBlockPre = sgGeneral.add(new IntSetting.Builder()
        .name("PlaceBlockPre")
        .description("The range to search for targets")
        .defaultValue(2)
        .sliderRange(1,8)
        .build()
    );
    private final Setting<Boolean> SwingHand = sgGeneral.add(new BoolSetting.Builder()
        .name("Swing-hand")
        .description("Swing your hand to bypass anti-cheat")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> Rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("Rotate")
        .description("Rotating your head to bypass anti-cheat (?)")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> CheckEntities = sgGeneral.add(new BoolSetting.Builder()
        .name("Check-entities")
        .description("I don't know...")
        .defaultValue(false)
        .build()
    );
    private final Setting<Integer> rotationPriority = sgGeneral.add(new IntSetting.Builder()
        .name("rotationPriority")
        .description("The range to search for targets")
        .defaultValue(2)
        .sliderRange(1,8)
        .build()
    );
    private final Setting<Boolean> InvSwap = sgGeneral.add(new BoolSetting.Builder()
        .name("Inventory-swap")
        .description("Swaping item in inventory")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> SwapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("Swap-back")
        .description("Swaping item back to inventory")
        .defaultValue(false)
        .build()
    );
    public Debug_Placement() { super("Debug_Placement", "debug feature"); }
    BlockPos playerPos;
    FindItemResult obsidianResult;
    int obsidianSolt;
    @EventHandler
    private void onTick (TickEvent.Post event) {
        msg("坐标是从玩家自身坐标开始进行偏移计算！");
        PlaceBlock();
        toggle();
    }
    private void PlaceBlock() {
        if (mc.player == null) return;
        obsidianResult = InvUtils.find(Items.OBSIDIAN);
        obsidianSolt = InvUtils.find(Items.OBSIDIAN).slot();
        msg(obsidianSolt);
        playerPos = mc.player.getBlockPos();
        NInvUtils.swap(obsidianSolt,1 , true);
        for (int i = 0; i < PlaceBlockPre.get(); i++) {
            BlockUtils.place(playerPos.add(XOffset.get(), YOffset.get(), ZOffset.get()), obsidianResult, Rotate.get(), rotationPriority.get() ,SwingHand.get(), CheckEntities.get());
        }
        if(InvSwap.get() && SwapBack.get()) NInvUtils.swapBack();
    }
}
