package com.omsent.addon.modules;

import com.ibm.icu.util.Output;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.Items;
import org.spongepowered.asm.service.modlauncher.ModLauncherAuditTrail;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

public class AnchorHelper extends NModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // 渲染锚列表
    private final List<RenderAnchor> renderAnchors = new ArrayList<>();

    // 仅限该模块的公开性变量
    int private_anchorSlot = 0;
    boolean ModuleClose;

    // 渲染锚类
    private static class RenderAnchor {
        public final BlockPos pos;
        public int life;
        public static final int MAX_LIFE = 40; // 最大生命周期（刻）

        public RenderAnchor(BlockPos pos) {

            this.pos = pos;
            this.life = MAX_LIFE;
        }
    }

    enum PlaceMode {
        Around,
        AroundPlus,
        Head,
        Feet,
        All,
        FeetAndHead,
    }

    // Swap mode enum
    enum SwapMode {
        NormalSwap,
        InventorySilentSwap
    }

    enum NoblockAction { //当没有方块时要做的操作
        ToggleModule, //切换模块
        Notify, //通知玩家
        None //不执行任何操作
    }

    enum blockChecktype { //要检查的方块类型
        OnlyAnchor,
        OnlyGlowStone,
        All,
    }

    private enum AH_type {
        Anchor,
        GlowStone,
        None,
    }

    public AnchorHelper() {
        super("AnchorHelper", "Places anchors at the feet of the target");
    }

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("Range")
        .description("The range to search for targets")
        .defaultValue(5)
        .min(1.0)
        .max(256.0)
        .sliderRange(1.0, 7.0)
        .build()
    );

    private final Setting<PlaceMode> placeMode = sgGeneral.add(new EnumSetting.Builder<PlaceMode>()
        .name("Place Mode")
        .description("The mode to place anchors")
        .defaultValue(PlaceMode.Around)
        .build()
    );

    private final Setting<SortPriority> targetPriority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How to select the player to target.")
        .defaultValue(SortPriority.LowestDistance)
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
    private final Setting<Integer> BlockPrePlace = sgGeneral.add(new IntSetting.Builder()
        .name("Block Pre-Place")
        .description("The block distance to place anchors")
        .defaultValue(2)
        .min(1)
        .max(8)
        .sliderRange(1, 8)
        .build()
    );
    private final Setting<Boolean> blockCheck = sgGeneral.add(new BoolSetting.Builder()
        .name("Block Check")
        .description("Check for blocks before placing anchors")
        .defaultValue(true)
        .build()
    );
    private final Setting<NoblockAction> BlockCheckMode = sgGeneral.add(new EnumSetting.Builder<NoblockAction>()
        .name("When no block")
        .description("What to do when no block is found")
        .visible(blockCheck::get)
        .defaultValue(NoblockAction.None)
        .build()
    );
    private final Setting<blockChecktype> blockCheckType = sgGeneral.add(new EnumSetting.Builder<blockChecktype>()
        .name("Block check type")
        .description("The type to check for blocks")
        .visible(blockCheck::get)
        .defaultValue(blockChecktype.All)
        .build()
    );
    private final Setting<Boolean> staticAnchorSlot = sgGeneral.add(new BoolSetting.Builder()
        .name("StaticSlot")
        .description("Static anchor to hotbar")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> staticSlot = sgGeneral.add(new IntSetting.Builder()
        .name("Static slot to")
        .description("Static anchor to hotbar")
        .min(1)
        .max(8)
        .sliderRange(1, 8)
        .build()
    );
    private final Setting<Integer> PreSlot = sgGeneral.add(new IntSetting.Builder()
        .name("Pre switch item")
        .description("Pre switch item")
        .min(1)
        .max(5)
        .defaultValue(3)
        .sliderRange(1, 5)
        .build()
    );
    private final Setting<Boolean> itemSwitchProtect = sgGeneral.add(new BoolSetting.Builder()
        .name("Item switch protect")
        .description("?")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> Debug = sgGeneral.add(new BoolSetting.Builder()
        .name("Debug")
        .description("Enable debug mode")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> Debug_TickEvent = sgGeneral.add(new BoolSetting.Builder()
        .name("Debug.TickEvent")
        .description("debug")
        .visible(Debug::get)
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> Debug_Placement = sgGeneral.add(new BoolSetting.Builder()
        .name("Debug.Placement")
        .description("debug")
        .visible(Debug::get)
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> Debug_searchTarget = sgGeneral.add(new BoolSetting.Builder()
        .name("Debug.Target.search")
        .description("debug")
        .visible(Debug::get)
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> Debug_searchTarget_searchable = sgGeneral.add(new BoolSetting.Builder()
        .name("Debug.Target.search.searchable")
        .description("debug")
        .visible(Debug_searchTarget::get)
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> Debug_StaticItem_Swap = sgGeneral.add(new BoolSetting.Builder()
        .name("Debug.Setting.staticAnchor.swap")
        .description("debug")
        .visible(Debug::get)
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> Debug_Function_AHTypeResultMaker_output = sgGeneral.add(new BoolSetting.Builder()
        .name("Debug.Function.AHTypeResultMaker.output")
        .description("debug")
        .visible(Debug::get)
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> Debug_ItemSlot = sgGeneral.add(new BoolSetting.Builder()
        .name("Debug.ItemSlot")
        .description("debug")
        .visible(Debug::get)
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> Debug_Module_Enable = sgGeneral.add(new BoolSetting.Builder()
        .name("Debug.Module.Enable")
        .description("debug")
        .visible(Debug::get)
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> Debug_Module_Disable = sgGeneral.add(new BoolSetting.Builder()
        .name("Debug.Module.Disable")
        .description("debug")
        .visible(Debug::get)
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> Debug_General_whileEvent = sgGeneral.add(new BoolSetting.Builder()
        .name("Debug.General.while.trigger")
        .description("debug")
        .visible(Debug::get)
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> Render = sgRender.add(new BoolSetting.Builder()
        .name("Render")
        .description("Enable anchors render")
        .defaultValue(false)
        .build()
    );
    private final Setting<SettingColor> boxColor = sgRender.add(new ColorSetting.Builder()
        .name("Box Color")
        .description("The color of the anchors")
        .defaultValue(new SettingColor(255, 0, 0, 50))
        .visible(Render::get)
        .build()
    );

    private final Setting<SettingColor> fillColor = sgRender.add(new ColorSetting.Builder()
        .name("Fill Color")
        .description("The fill color of the anchors")
        .defaultValue(new SettingColor(255, 0, 0, 50))
        .visible(Render::get)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("Shape Mode")
        .description("How the shapes are rendered")
        .defaultValue(ShapeMode.Both)
        .visible(Render::get)
        .build()
    );

    private final Setting<Integer> FadeSpeed = sgRender.add(new IntSetting.Builder()
        .name("Fade Speed")
        .description("The speed at which the anchors fade")
        .defaultValue(1)
        .min(1)
        .max(10)
        .sliderRange(1, 10)
        .visible(Render::get)
        .build()
    );


    @Override
    public void onActivate() { //当打开功能
        if (!Main.enable) {
            toggle();
            return;
        }
        if (Debug_Module_Disable.get() == true) {
            OutputDebugInfo("§aSucceed toggle module (Enable)!");
        }
        //设置模块开启状态
        ModuleClose = false;
    }

    @Override
    public void onDeactivate() { //当关闭功能
        //设置模块关闭状态
        ModuleClose = true;
        if (Debug_Module_Disable.get() == true) {
            OutputDebugInfo("§cModule disabled!");
        }
        for (int i = 1;i <= PreSlot.get();i++) {
            InvUtils.move().from(staticSlot.get()).to(private_anchorSlot);
        }
    }

    // 在Tick事件中执行循环逻辑，不会阻塞主线程
    @EventHandler
    private void onLoopTick(TickEvent.Post event) {
        if (ModuleClose) return;
        if (Debug_General_whileEvent.get() == true) OutputDebugInfo("Tick event trigger! (onLoopTick)");
        // 重构的物品固定器
        if (staticAnchorSlot.get() == true) {
            private_anchorSlot = InvUtils.find(Items.RESPAWN_ANCHOR).slot();
            if (private_anchorSlot != -1) {
                InvUtils.move().from(private_anchorSlot).to(staticSlot.get());
            }
        }
        if (itemSwitchProtect.get() == true) InvUtils.move().from(staticSlot.get()).to(private_anchorSlot);
    }
    @EventHandler
    private void ItemProtect(TickEvent.Pre event) throws InterruptedException {
        if(!itemSwitchProtect.get() || ModuleClose) return;
        for (int i = 1;i <= PreSlot.get();i++) {
            InvUtils.move().from(staticSlot.get()).to(private_anchorSlot);
        }
        for (int i = 1;i <= PreSlot.get();i++) {
            InvUtils.move().from(staticSlot.get()).to(private_anchorSlot);
        }
    }
    /**
     * 搜索距离玩家最近的目标
     */
    @EventHandler
    private void OnRender3D(Render3DEvent event) {
        if (!Render.get()) return;

        // 更新渲染锚的生命周期
        renderAnchors.removeIf(anchor -> {
            anchor.life -= FadeSpeed.get();
            return anchor.life <= 0;
        });

        // 渲染锚
        for (RenderAnchor anchor : renderAnchors) {
            // 计算透明度
            float alpha = (float) anchor.life / RenderAnchor.MAX_LIFE;
            alpha = Math.clamp(alpha, 0, 1);

            // 创建颜色（使用设置的颜色并应用透明度）
            Color side = new Color(
                fillColor.get().r,
                fillColor.get().g,
                fillColor.get().b,
                (int) (fillColor.get().a * alpha)
            );

            Color line = new Color(
                boxColor.get().r,
                boxColor.get().g,
                boxColor.get().b,
                (int) (boxColor.get().a * alpha)
            );

            // 渲染方块
            event.renderer.box(
                anchor.pos,
                side,
                line,
                shapeMode.get(),
                0
            );
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!Main.enable) {
            toggle();
            return;
        }
        if (Debug_TickEvent.get()) OutputDebugInfo("Ontick event trigger!");
        LivingEntity target = findtarget();
        if (target == null) return;
        placeAnchorsAtTargetFeet(target);
    }

    public LivingEntity findtarget() {
        if (mc.player == null) {
            return null;
        }

        // 使用TargetUtils获取最近的目标
        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), targetPriority.get());
        if (target == null) {
            if (Debug_searchTarget.get()) OutputDebugInfo("Target is null!");
            return null;
        }
        //如果搜索到的目标是空的 则直接取消操作
        //判断是不是在彗星白名单里面
        boolean isFriend = Friends.get().isFriend(target);
        if (isFriend) return null;
        if (Debug_searchTarget_searchable.get())
            OutputDebugInfo("Found Target: Pos: X:" + target.getBlockPos().getX() + "Y:" + target.getBlockPos().getY() + "Z" + target.getBlockPos().getZ() + ", Name:" + target.getName());
        return target;
    }

    /**
     * /
     * 这个!Objects.equals(text, "Place Pos: {}")
     * 是为了防止出现无效的Placement Debug info(放置调试信息)
     */
    public void OutputDebugInfo(String text) {
        if (Debug.get() && !Objects.equals(text, "Place Pos: {}")) {
            msg(text);
        }
    }

    public void warn(String text) {
        msg("[§l§e!§r] " + text);
    }

    //当没有找到方块时进行的操作
    private AH_type AH_TypeResultMaker(int AnchorStat, int GlowStoneStat) {
        AH_type Result = AH_type.None;
        if (AnchorStat == -1) {
            Result = AH_type.Anchor;
        } else if (GlowStoneStat == -1) {
            Result = AH_type.GlowStone;
        }
        if (Debug_Function_AHTypeResultMaker_output.get())
            OutputDebugInfo("AH_TypeResultMaker: arg.AnchorStat=" + AnchorStat + ", arg.GlowStoneStat=" + GlowStoneStat);
        return Result;
    }

    private void whenCheckItemFailed(@Nullable AH_type Type) {
        NoblockAction CheckMode;
        String messageContext = null;
        if (blockCheckType.get() == blockChecktype.OnlyAnchor) {
            messageContext = "Respawn anchor not found!";
        } else if (blockCheckType.get() == blockChecktype.OnlyGlowStone) {
            messageContext = "Glow Stone not found!";
        } else if (blockCheckType.get() == blockChecktype.All && Type != null) {
            if (Type == AH_type.Anchor) {
                messageContext = "Respawn anchor not found!";
            } else if (Type == AH_type.GlowStone) {
                messageContext = "Glow Stone not found!";
            }
        }
        CheckMode = BlockCheckMode.get();
        if (blockCheck.get()) {
            if (CheckMode == NoblockAction.ToggleModule) {
                toggle();
            } else if (CheckMode == NoblockAction.Notify) {
                warn(messageContext);
            }
        }
    }

    /**
     * 在目标脚边四个位置放置重生锚
     *
     * @param target 目标实体
     */
    public void placeAnchorsAtTargetFeet(LivingEntity target) {
        if (mc.player == null || target == null) return;

        // 获取目标脚边的位置
        BlockPos targetPos = target.getBlockPos();

        // 定义四个脚边位置
        BlockPos[] feetPositions = {
            targetPos.north(),
            targetPos.south(),
            targetPos.east(),
            targetPos.west()
        };

//        // 查找重生锚
//        int anchorSlot = 0;
//        int glowStoneDustSlot = 0;
//        glowStoneDustSlot = InvUtils.findInHotbar(Items.GLOWSTONE_DUST).slot();
//        anchorSlot = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR).slot();
//        FindItemResult anchorResult = InvUtils.find(Items.RESPAWN_ANCHOR);
//        if (anchorSlot == -1) {
//            // 在整个物品栏中查找
//            anchorSlot = InvUtils.find(Items.RESPAWN_ANCHOR).slot();
//            if (anchorSlot == -1 || glowStoneDustSlot == -1) {
//                return; // 没有重生锚或发光石粉
//            }
//        }


        // 在每个位置尝试放置重生锚
//        for (BlockPos pos : feetPositions) {
//            // 检查位置是否可以放置
//            if (mc.world != null && mc.world.getBlockState(pos).isAir()) {
//                // 使用BlockUtils放置重生锚
//                if (BlockUtils.canPlace(pos)) {
//                    BlockUtils.place(
//                        pos,
//                        anchorResult,
//                        0,
//                        false
//                    );
//                }
//            }
//        }

        //下面是新版本的检查逻辑
        int glowStoneDustSlot = InvUtils.find(Items.GLOWSTONE).slot();
        int anchorSlot = InvUtils.find(Items.RESPAWN_ANCHOR).slot();
        FindItemResult anchorResult = InvUtils.find(Items.RESPAWN_ANCHOR);
        if (Debug_ItemSlot.get())
            OutputDebugInfo("AnchorSlot: " + anchorSlot + ", GlowStoneSlot: " + glowStoneDustSlot);
        if (blockCheck.get()) {
            if (blockCheckType.get() == blockChecktype.OnlyAnchor && BlockCheckMode.get() != NoblockAction.None) {
                if (anchorSlot == -1) {
                    whenCheckItemFailed(null);
                    return;
                }//没有找到重生锚直接返回

            } else if (blockCheckType.get() == blockChecktype.OnlyGlowStone) {
                if (glowStoneDustSlot == -1 && BlockCheckMode.get() != NoblockAction.None) {
                    whenCheckItemFailed(null);
                    return;
                } //没有找到荧石直接返回
            } else if (blockCheckType.get() == blockChecktype.All) {
                if (glowStoneDustSlot == -1 || anchorSlot == -1) {
                    whenCheckItemFailed(AH_TypeResultMaker(anchorSlot, glowStoneDustSlot));
                    return;
                }
            }
        }


        String DebugInfo;
        DebugInfo = "";
        if (placeMode.get() == PlaceMode.Around) { //围绕辅助塞脚
            BlockPos[] positions = {
                targetPos.add(1, 0, 0),
                targetPos.add(-1, 0, 0),
                targetPos.add(0, 0, 1),
                targetPos.add(0, 0, -1)
            };
            for (BlockPos pos : positions) {
                if (!BlockUtils.canPlace(pos)) continue;
                DebugInfo = DebugInfo + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "} {";
                for (int i = 1; i <= BlockPrePlace.get(); i++) {
                    BlockUtils.place(pos, anchorResult, Rotate.get(), 1, SwingHand.get(), CheckEntities.get());
                }
                try {
                    if (renderAnchors.contains(pos)) {
                        return;
                    }
                } catch (NullPointerException | ClassCastException e) {
                    error(e.getMessage());
                }
                renderAnchors.add(new RenderAnchor(pos));
            }
            if (Debug_Placement.get()) OutputDebugInfo("Place Pos: {" + DebugInfo + "}");
        } else if (placeMode.get() == PlaceMode.Head) { //赛头
            BlockPos pos = targetPos.add(0, 2, 0);
            if (!BlockUtils.canPlace(pos)) return;
            for (int i = 1; i <= BlockPrePlace.get(); i++) {
                BlockUtils.place(pos, anchorResult, Rotate.get(), 1, SwingHand.get(), CheckEntities.get());
            }
            renderAnchors.add(new RenderAnchor(pos));
            if (Debug_Placement.get()) OutputDebugInfo("Place Pos: {" + DebugInfo + "}");
        } else if (placeMode.get() == PlaceMode.Feet) { //赛脚
            BlockPos pos = targetPos.add(0, -1, 0);
            if (!BlockUtils.canPlace(pos)) return;
            for (int i = 1; i <= BlockPrePlace.get(); i++) {
                BlockUtils.place(pos, anchorResult, Rotate.get(), 1, SwingHand.get(), CheckEntities.get());
            }
            try {
                if (renderAnchors.contains(pos)) {
                    return;
                }
            } catch (NullPointerException | ClassCastException e) {
                error(e.getMessage());
            }
            renderAnchors.add(new RenderAnchor(pos));
            if (Debug_Placement.get()) OutputDebugInfo("Place Pos: {" + DebugInfo + "}");
        } else if (placeMode.get() == PlaceMode.All) { //全都塞
            BlockPos[] positions = {
                // 上面的四个位置
                targetPos.add(1, 1, 0),
                targetPos.add(-1, 1, 0),
                targetPos.add(0, 1, 1),
                targetPos.add(0, 1, -1),
                // 下面的四个位置
                targetPos.add(1, 0, 0),
                targetPos.add(-1, 0, 0),
                targetPos.add(0, 0, 1),
                targetPos.add(0, 0, -1),
                //头
                targetPos.add(0, 2, 0),
                //脚
                targetPos.add(0, -1, 0),
            };
            for (BlockPos pos : positions) {
                if (!BlockUtils.canPlace(pos)) continue;
                DebugInfo = DebugInfo + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "} {";
                for (int i = 1; i <= BlockPrePlace.get(); i++) {
                    BlockUtils.place(pos, anchorResult, Rotate.get(), 1, SwingHand.get(), CheckEntities.get());
                }
                try {
                    if (renderAnchors.contains(pos)) {
                        return;
                    }
                } catch (NullPointerException | ClassCastException e) {
                    error(e.getMessage());
                }
                renderAnchors.add(new RenderAnchor(pos));
            }
            if (Debug_Placement.get()) OutputDebugInfo("Place Pos: {" + DebugInfo + "}");
        } else if (placeMode.get() == PlaceMode.FeetAndHead) {
            BlockPos[] positions = {
                targetPos.add(0, -1, 0), // 脚
                targetPos.add(0, 2, 0)  // 头
            };
            for (BlockPos pos : positions) {
                if (!BlockUtils.canPlace(pos)) continue;
                DebugInfo = DebugInfo + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "} {";
                for (int i = 1; i <= BlockPrePlace.get(); i++) {
                    BlockUtils.place(pos, anchorResult, Rotate.get(), 1, SwingHand.get(), CheckEntities.get());
                }
                try {
                    if (renderAnchors.contains(pos)) {
                        return;
                    }
                } catch (NullPointerException | ClassCastException e) {
                    error(e.getMessage());
                }
                renderAnchors.add(new RenderAnchor(pos));
            }
            if (Debug_Placement.get()) OutputDebugInfo("Place Pos: {" + DebugInfo + "}");
        } else if (placeMode.get() == PlaceMode.AroundPlus) {
            BlockPos[] positions = {
                targetPos.add(0, -1, 0), // 脚
                targetPos.add(0, 2, 0),  // 头
                targetPos.add(1, 0, 0), //下面的是Around
                targetPos.add(-1, 0, 0),
                targetPos.add(0, 0, 1),
                targetPos.add(0, 0, -1)
            };
            for (BlockPos pos : positions) {
                if (!BlockUtils.canPlace(pos)) continue;
                DebugInfo = DebugInfo + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "} {";
                for (int i = 1; i <= BlockPrePlace.get(); i++) {
                    BlockUtils.place(pos, anchorResult, Rotate.get(), 1, SwingHand.get(), CheckEntities.get());
                }
                try {
                    if (renderAnchors.contains(pos)) {
                        return;
                    }
                } catch (NullPointerException | ClassCastException e) {
                    error(e.getMessage());
                }
                renderAnchors.add(new RenderAnchor(pos));
            }
            if (Debug_Placement.get()) OutputDebugInfo("Place Pos: {" + DebugInfo + "}");
        }
    }
}
