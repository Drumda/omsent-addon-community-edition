package com.omsent.addon.modules;

import com.ibm.icu.util.Output;
import com.omsent.addon.NModule;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.WallRedstoneTorchBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.ObjectUtils;
import org.joml.Vector3d;
import net.minecraft.item.SwordItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static meteordevelopment.meteorclient.utils.entity.DamageUtils.crystalDamage;

public class CrystalBooster extends NModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgDebug = settings.createGroup("Debug");
    private final SettingGroup sgOther = settings.createGroup("Other");

    // 攻击范围设置，最小值3，最大值7
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("Range")
        .description("Auto break range of end crystal")
        .sliderRange(3, 7)
        .defaultValue(5)
        .build()
    );

    // 安全范围设置，在此范围内的末地水晶不会被攻击
    private final Setting<Double> safeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("Safe Range")
        .description("End crystals within this range will not be attacked")
        .sliderRange(0, 5)
        .defaultValue(0)
        .build()
    );
    //伤害检测

    private final Setting<Boolean> DamageCheck = sgGeneral.add(new BoolSetting.Builder()
        .name("Damage Check")
        .description("Check damage of crystal before attack")
        .defaultValue(true)
        .build()
    );
    //伤害阈值
    private final Setting<Double> BreakDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("Break Damage")
        .description("Damage threshold to break crystal")
        .sliderRange(0.1,20.0 )
        .defaultValue(8.0)
        .build()
    );
    // 智能变速设置
    private final Setting<Boolean> smartSpeed = sgGeneral.add(new BoolSetting.Builder()
        .name("Smart Speed")
        .description("Automatically adjust attack speed based on crystal distance")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> feetHelper = sgGeneral.add(new BoolSetting.Builder()
        .name("Feet helper")
        .description("")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> feetHelper_rangeCheck = sgGeneral.add(new BoolSetting.Builder()
        .name("Feet helper / Range check")
        .description("")
        .defaultValue(true)
        .visible(feetHelper::get)
        .build()
    );
    private final Setting<Integer> feetHelper_range = sgGeneral.add(new IntSetting.Builder()
        .name("Feet helper / Range")
        .description("")
        .defaultValue(6)
        .min(1)
        .max(6)
        .sliderRange(1, 6)
        .visible(feetHelper_rangeCheck::get)
        .build()
    );
    private final Setting<Boolean> staticHelpPos = sgGeneral.add(new BoolSetting.Builder()
        .name("[Bad] Feet helper / Static help pos")
        .description("Static help pos")
        .defaultValue(false)
        .visible(feetHelper::get)
        .build()
    );
    private final Setting<Double> targetBreakDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("Target break damage")
        .description("Making damage for target")
        .defaultValue(5.0)
        .min(0.0)
        .max(36.0)
        .sliderRange(1.0, 36.0)
        .visible(feetHelper::get)
        .build()
    );
    private final Setting<Integer> PreCrystalPlace = sgGeneral.add(new IntSetting.Builder()
        .name("Crystal pre place")
        .description("?")
        .defaultValue(5)
        .min(1)
        .max(36)
        .sliderRange(1, 36)
        .build()
    );
    private final Setting<SortPriority> targetPriority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How to select the player to target.")
        .defaultValue(SortPriority.LowestDistance)
        .visible(feetHelper::get)
        .build()
    );
    // 智能变速距离阈值
    private final Setting<Double> speedThreshold = sgGeneral.add(new DoubleSetting.Builder()
        .name("Smart / Check Range")
        .description("Distance threshold for smart speed adjustment")
        .visible(smartSpeed::get)
        .sliderRange(1, 5)
        .defaultValue(2)
        .build()
    );

    // 减速系数
    private final Setting<Double> slowFactor = sgGeneral.add(new DoubleSetting.Builder()
        .name("Smart / Slow Factor")
        .description("Factor to slow down attack speed when crystal is close")
        .visible(smartSpeed::get)
        .sliderRange(0.0, 20.0)
        .defaultValue(2)
        .build()
    );

    // 智能模式设置
    private final Setting<Boolean> smart = sgGeneral.add(new BoolSetting.Builder()
        .name("Smart")
        .description("Pause when holding sword or tools")
        .defaultValue(true)
        .build()
    );

    // 攻击速度设置
    private final Setting<Integer> attackSpeed = sgGeneral.add(new IntSetting.Builder()
        .name("AttackSpeed")
        .description("Auto break speed of end crystal")
        .min(1)
        .max(10)
        .sliderRange(1, 10)
        .defaultValue(2)
        .build()
    );

    // 每次攻击次数设置
    private final Setting<Integer> attackCount = sgGeneral.add(new IntSetting.Builder()
        .name("AttackCount")
        .description("Auto break count of end crystal")
        .min(1)
        .max(20)
        .sliderRange(1, 30)
        .defaultValue(1)
        .build()
    );

    // Debug模式设置
    private final Setting<Boolean> debug = sgDebug.add(new BoolSetting.Builder()
        .name("Debug")
        .description("Debug mode for CrystalBreakHelper")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> RenderBlock = sgRender.add(new BoolSetting.Builder()
        .name("Render")
        .description("Render the blocks that are being broken.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .visible(RenderBlock::get)
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .visible(RenderBlock::get)
        .defaultValue(new SettingColor(204, 0, 0, 10))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .visible(RenderBlock::get)
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .build()
    );
    private final Setting<Integer> RenderYOffset = sgRender.add(new IntSetting.Builder()
        .name("RenderYOffset")
        .description("The Y offset of the rendered blocks.")
        .visible(RenderBlock::get)
        .min(-10)
        .max(10)
        .sliderRange(-10, 10)
        .defaultValue(-1)
        .build()
    );
    private final Setting<Integer> FadeSpeed = sgRender.add(new IntSetting.Builder()
        .name("FadeSpeed")
        .description("The fade speed of the rendered blocks.")
        .visible(RenderBlock::get)
        .min(0)
        .max(20)
        .sliderRange(0, 20)
        .defaultValue(1)
        .build()
    );

    // 文字显示设置
    private final Setting<Boolean> renderText = sgRender.add(new BoolSetting.Builder()
        .name("Render Text")
        .description("Render text at the crystal attack point")
        .defaultValue(true)
        .build()
    );

    // 字体大小设置
    private final Setting<Double> fontScale = sgRender.add(new DoubleSetting.Builder()
        .name("Font Scale")
        .description("The scale of the rendered text")
        .visible(renderText::get)
        .min(0.1)
        .max(3)
        .defaultValue(1)
        .build()
    );

    // 字体Y偏移设置
    private final Setting<Double> fontYOffset = sgRender.add(new DoubleSetting.Builder()
        .name("Font Y Offset")
        .description("The Y offset of the rendered text")
        .visible(renderText::get)
        .min(-5)
        .max(5)
        .defaultValue(0)
        .build()
    );

    // 文字颜色设置
    private final Setting<SettingColor> textColor = sgRender.add(new ColorSetting.Builder()
        .name("Text Color")
        .description("The color of the rendered text")
        .visible(renderText::get)
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private final Setting<Double> textYOffset = sgRender.add(new DoubleSetting.Builder()
        .name("Text Y Offset")
        .description("The Y offset of the rendered text")
        .visible(renderText::get)
        .min(-5.0)
        .max(5.0)
        .sliderRange(-5.0, 5.0)
        .defaultValue(0.0)
        .build()
    );
    // 文字之间的间隔
    private final Setting<Double> textInterval = sgRender.add(new DoubleSetting.Builder()
        .name("Text Interval")
        .description("The interval between the rendered text")
        .visible(renderText::get)
        .min(0.1)
        .max(6.0)
        .sliderRange(0.1, 6.0)
        .defaultValue(1.0)
        .build()
    );

    // 文字渐变速度设置
    private final Setting<Integer> textFadeSpeed = sgRender.add(new IntSetting.Builder()
        .name("Text Fade Speed")
        .description("The fade speed of the rendered text")
        .visible(renderText::get)
        .min(1)
        .max(20)
        .defaultValue(1)
        .build()
    );

    // ===== 方块搜索器设置 =====
    private final Setting<Boolean> blockBreaker = sgGeneral.add(new BoolSetting.Builder()
        .name("Piston helper")
        .description("Search for and destroy redstone blocks, torches, and pistons")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> breakerRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("Search range")
        .description("Search radius for blocks to destroy")
        .visible(blockBreaker::get)
        .sliderRange(1, 10)
        .defaultValue(5)
        .build()
    );

    private final Setting<Double> safeDamageThreshold = sgGeneral.add(new DoubleSetting.Builder()
        .name("Place / Break Damage")
        .description("Maximum damage allowed from crystal explosion")
        .visible(blockBreaker::get)
        .sliderRange(0.0, 2.0)
        .defaultValue(0.2)
        .build()
    );
    @Override
    public void onActivate() {
        if (!Main.enable) {
            toggle();
            return;
        }
    }
    // 计时器，用于控制攻击频率
    private int tickTimer = 0;

    // 攻击次数计数器
    private int currentAttackCount = 0;

    // 渲染方块列表
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    // 渲染文字列表
    private final List<RenderText> renderTexts = new ArrayList<>();

    // 塞脚辅助器辅助次数
    int currentHelperCount;

    // 当前进行操作的坐标
    EndCrystalEntity currentActionEntity;

    private final Vec3d vec3d = new Vec3d(0, 0, 0);

    // 方块搜索器相关
    private BlockPos currentTargetBlock = null;
    private int breakerTickTimer = 0;
    private final Set<BlockPos> processedBlocks = new HashSet<>();


    // 渲染方块类
    static class RenderBlock {
        public final BlockPos pos;
        public int life;
        public static final int MAX_LIFE = 20; // 最大生命周期（刻）

        public RenderBlock(BlockPos pos) {
            this.pos = pos;
            this.life = MAX_LIFE;
        }
    }

    // 渲染文字类
    private static class RenderText {
        public final Vec3d pos;
        public final String text;
        public int life;
        public static final int MAX_LIFE = 40; // 最大生命周期（刻）

        public RenderText(Vec3d pos, String text) {
            this.pos = pos;
            this.text = text;
            this.life = MAX_LIFE;
        }
    }
    public EndCrystalEntity targetCrystal;
    public CrystalBooster() {
        super("Crystal-Booster", "Booster the speed of end crystal");
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Main.enable) {
            toggle();
            return;
        }
        try {
            // 更新渲染方块的生命周期
            updateRenderBlocks();

            // 更新渲染文字的生命周期
            updateRenderTexts();

            // 检查是否手持剑或工具，如果是且开启了智能模式，则停止攻击
            if (smart.get() && mc.player != null) {
                boolean isHoldingSword = mc.player.getMainHandStack().getItem() instanceof SwordItem;

                if (isHoldingSword) {
                    if (debug.get()) {
                        msg("Smart mode active, holding sword/tool - skipping attack");
                    }
                    return;
                }
            }

            // 增加计时器
            tickTimer++;

            // 查找范围内的末地水晶
            targetCrystal = findCrystal();

            // 检查是否达到攻击频率
            int currentAttackSpeed = getCurrentAttackSpeed(targetCrystal);
            if (tickTimer % currentAttackSpeed != 0) {
                return;
            }

            // 重置计时器
            tickTimer = 0;

            // 如果找到水晶，则攻击
            if (targetCrystal != null) {
                if (debug.get()) {
                    msg("Found crystal at: " + targetCrystal.getPos());
                }
                attackCrystal(targetCrystal);

                // 添加文字渲染
                addRenderTexts(targetCrystal);
            } else if (debug.get()) {
                //msg("No crystals found in range");
            }
        } catch (Exception e) {
            // 处理错误
            error("Error occurred: " + e.getMessage());
            toggle();
        }
    }
    @EventHandler
    private void FeetHelperTickEvent(TickEvent.Post event) {
        if (staticHelpPos.get() == false) return;
        if (feetHelper.get() == false) return;
        if (currentActionEntity == null) return;

        // 查找末地水晶
        FindItemResult crystalResult = InvUtils.findInHotbar(Items.END_CRYSTAL);
        if (crystalResult.slot() == -1) {
            return;
        }

        // 获取目标位置（黑曜石上方，放置水晶的位置）
        BlockPos targetPos = currentActionEntity.getBlockPos().down().up();

        // 检查是否可以放置水晶
        if (!canPlaceCrystal(targetPos.down())) {
            if (debug.get()) {
                msg("Cannot place crystal at: " + targetPos);
            }
            return;
        }

        // 使用 BlockUtils 放置末地水晶
        PlayerEntity target = null;
        if (feetHelper_rangeCheck.get() == true) target = TargetUtils.getPlayerTarget(range.get(), targetPriority.get());
        if (target == null && feetHelper_rangeCheck.get()) return;
        BlockUtils.place(targetPos, crystalResult, false, 0, true, true);
        if (debug.get()) {
            if (target != null) {
                msg("Placed crystal at: " + targetPos);
            } else {
                OutputDebugInfo("No target found!");
            }
        }
    }

    // ===== 方块搜索器功能 =====
    @EventHandler
    private void onBlockBreakerTick(TickEvent.Post event) {
        if (!blockBreaker.get()) return;
        if (mc.player == null || mc.world == null) return;

        breakerTickTimer++;

        // 每5tick执行一次搜索（提高频率）
        if (breakerTickTimer % 5 != 0) return;

        breakerTickTimer = 0;

        // 清理已破坏的方块记录
        cleanupProcessedBlocks();

        // 搜索目标方块
        List<BlockPos> targetBlocks = searchTargetBlocks();

        if (debug.get()) {
            msg("Found " + targetBlocks.size() + " target blocks");
        }

        if (targetBlocks.isEmpty()) {
            currentTargetBlock = null;
            return;
        }

        // 按距离排序，优先处理近处的方块
        targetBlocks.sort((a, b) -> {
            double distA = a.getSquaredDistance(mc.player.getBlockPos());
            double distB = b.getSquaredDistance(mc.player.getBlockPos());
            return Double.compare(distA, distB);
        });

        // 找到目标后尝试放置水晶（每次处理多个）
        int processedThisTick = 0;
        int maxPerTick = 3; // 每tick最多处理3个方块

        for (BlockPos blockPos : targetBlocks) {
            if (processedBlocks.contains(blockPos)) continue;
            if (processedThisTick >= maxPerTick) break;

            if (tryPlaceCrystalToBreak(blockPos)) {
                processedBlocks.add(blockPos);
                processedThisTick++;
                if (debug.get()) {
                    msg("Placed crystal to break block at: " + blockPos);
                }
            }
        }
    }

    // 清理已破坏的方块记录
    private void cleanupProcessedBlocks() {
        processedBlocks.removeIf(pos -> {
            if (mc.world == null) return true;
            // 如果方块已经不存在（被破坏了），则移除记录
            return !isTargetBlock(pos);
        });
    }

    // 搜索目标方块（红石块、红石火把、活塞、粘性活塞）
    private List<BlockPos> searchTargetBlocks() {
        List<BlockPos> targets = new ArrayList<>();

        if (mc.player == null || mc.world == null) return targets;

        BlockPos playerPos = mc.player.getBlockPos();
        int range = (int) Math.ceil(breakerRange.get());

        // 在半径范围内搜索
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.add(x, y, z);

                    // 检查是否在搜索半径内
                    if (pos.getSquaredDistance(playerPos) > breakerRange.get() * breakerRange.get()) {
                        continue;
                    }

                    // 检查是否是目标方块
                    if (isTargetBlock(pos)) {
                        targets.add(pos);
                    }
                }
            }
        }

        return targets;
    }

    // 检查方块是否是目标方块
    private boolean isTargetBlock(BlockPos pos) {
        if (mc.world == null) return false;

        Block block = mc.world.getBlockState(pos).getBlock();

        // 检查红石块
        if (block == Blocks.REDSTONE_BLOCK) {
            if (debug.get()) msg("Found REDSTONE_BLOCK at: " + pos);
            return true;
        }

        // 检查红石火把（地上和墙上）
        if (block instanceof RedstoneTorchBlock || block instanceof WallRedstoneTorchBlock) {
            if (debug.get()) msg("Found REDSTONE_TORCH at: " + pos);
            return true;
        }

        // 检查活塞
        if (block instanceof PistonBlock) {
            if (debug.get()) msg("Found PISTON at: " + pos);
            return true;
        }

        // 检查粘性活塞
        if (block == Blocks.STICKY_PISTON) {
            if (debug.get()) msg("Found STICKY_PISTON at: " + pos);
            return true;
        }

        return false;
    }

    // 尝试放置水晶炸掉目标方块
    private boolean tryPlaceCrystalToBreak(BlockPos targetBlock) {
        if (mc.player == null || mc.world == null) return false;
        FindItemResult inventoryCrystal = null;
        // 检查目标方块是否仍然存在
        if (!isTargetBlock(targetBlock)) {
            if (debug.get()) msg("Target block no longer exists: " + targetBlock);
            return false;
        }

        // 查找末地水晶
        FindItemResult crystalResult = InvUtils.findInHotbar(Items.END_CRYSTAL);
        if (crystalResult.slot() == -1) {
            // 尝试从背包中找水晶并移动到快捷栏
            inventoryCrystal = InvUtils.find(Items.END_CRYSTAL);
            if (inventoryCrystal.slot() != -1) {
                InvUtils.move().from(inventoryCrystal.slot()).toHotbar(1);
                crystalResult = InvUtils.findInHotbar(Items.END_CRYSTAL);
            }
            if (crystalResult.slot() == -1) {
                if (debug.get()) msg("No END_CRYSTAL found");
                return false;
            }
        }

        // 找到合适的放置位置
        BlockPos crystalPos = findSafeCrystalPosition(targetBlock);
        if (crystalPos == null) {
            if (debug.get()) msg("No safe crystal position found for: " + targetBlock);
            return false;
        }

        // 检查伤害是否安全
        double damage = crystalDamage(mc.player, crystalPos.toCenterPos());
        if (damage > safeDamageThreshold.get()) {
            if (debug.get()) {
                msg("Damage too high: " + damage + " > " + safeDamageThreshold.get());
            }
            return false;
        }

        if (debug.get()) {
            msg("Placing crystal at: " + crystalPos + " to break: " + targetBlock);
        }

        // 放置水晶
        BlockUtils.place(crystalPos, crystalResult, false, 0, true, false);

        if (debug.get()) {
            msg("Placed crystal at " + crystalPos + " to break block at " + targetBlock);
        }
        crystalResult = InvUtils.findInHotbar(Items.END_CRYSTAL);
        if (crystalResult.slot() == -1) return false;
        if (inventoryCrystal == null) return false;
        try {
            InvUtils.move().from(crystalResult.slot()).to(inventoryCrystal.slot());
        } catch (NullPointerException e) {
            OutputDebugInfo(e.getMessage());
        }

        return true;
    }

    // 找到安全的水晶放置位置
    private BlockPos findSafeCrystalPosition(BlockPos targetBlock) {
        if (mc.world == null) return null;

        // 尝试在目标方块上方放置水晶（不需要黑曜石底座）
        BlockPos abovePos = targetBlock.up();
        if (canPlaceCrystalAt(abovePos)) {
            if (debug.get()) msg("Found position above target block: " + abovePos);
            return abovePos;
        }

        // 尝试在目标方块周围找黑曜石底座
        BlockPos[] offsets = {
            new BlockPos(0, 0, 0),
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(0, 0, -1),
            new BlockPos(1, 0, 1),
            new BlockPos(1, 0, -1),
            new BlockPos(-1, 0, 1),
            new BlockPos(-1, 0, -1)
        };

        for (BlockPos offset : offsets) {
            BlockPos checkPos = targetBlock.add(offset);

            // 检查是否有黑曜石底座
            if (mc.world.getBlockState(checkPos).getBlock() == Blocks.OBSIDIAN) {
                BlockPos crystalPos = checkPos.up();
                if (canPlaceCrystalAt(crystalPos)) {
                    if (debug.get()) msg("Found obsidian base at: " + checkPos);
                    return crystalPos;
                }
            }
        }

        // 如果没有黑曜石底座，尝试在目标方块周围找空地放置
        for (BlockPos offset : offsets) {
            BlockPos checkPos = targetBlock.add(offset);
            if (canPlaceCrystalAt(checkPos)) {
                if (debug.get()) msg("Found empty position at: " + checkPos);
                return checkPos;
            }
        }

        return null;
    }

    // 检查是否可以在指定位置放置水晶
    private boolean canPlaceCrystalAt(BlockPos pos) {
        if (mc.world == null) return false;

        // 检查位置是否为空
        if (!mc.world.getBlockState(pos).isAir()) return false;

        // 检查上方是否有空隙
        BlockPos doubleAbove = pos.up();
        if (!mc.world.getBlockState(doubleAbove).isAir()) return false;

        // 检查是否有实体
        Box box = new Box(pos);
        if (!mc.world.getEntitiesByClass(Entity.class, box, e -> true).isEmpty()) return false;

        return true;
    }

    // 检查是否可以在指定位置放置末地水晶（检查黑曜石底座）
    private boolean canPlaceCrystal(BlockPos obsidianPos) {
        if (mc.world == null) return false;

        // 检查底座是否是黑曜石
        if (mc.world.getBlockState(obsidianPos).getBlock() != Blocks.OBSIDIAN) {
            return false;
        }

        // 检查上方是否为空
        BlockPos upPos = obsidianPos.up();
        if (!mc.world.getBlockState(upPos).isAir()) {
            return false;
        }

        // 检查上方是否有实体
        Box upBox = new Box(upPos);
        if (!mc.world.getEntitiesByClass(
            Entity.class,
            upBox,
            e -> true
        ).isEmpty()) {
            return false;
        }

        return true;
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        // 渲染方块
        renderBlocks(event);
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        // 渲染文字
        if (renderText.get() && mc.player != null) {
            renderText2D();
        }
    }

    /**
     * 更新渲染方块的生命周期
     */
    private void updateRenderBlocks() {
        renderBlocks.removeIf(block -> {
            block.life -= FadeSpeed.get();
            return block.life <= 0;
        });
    }

    /**
     * 更新渲染文字的生命周期
     */
    private void updateRenderTexts() {
        renderTexts.removeIf(text -> {
            text.life -= textFadeSpeed.get();
            return text.life <= 0;
        });
    }

    /**
     * 添加渲染文字
     */
    private void addRenderTexts(EndCrystalEntity crystal) {
        if (!renderText.get() || crystal == null) return;

        // 计算当前攻击速度
        int currentAttackSpeed = getCurrentAttackSpeed(crystal);

        // 计算剩余tick
        int remainingTicks = currentAttackSpeed;

        // 计算水晶伤害
        float crystalDamage = 0;
        if (mc.player != null) {
            crystalDamage = crystalDamage(mc.player, crystal.getPos());
        }

        // 获取水晶位置
        Vec3d targetPos = crystal.getPos();
        double distance = mc.player.getPos().distanceTo(targetPos);

        // 计算文字间隔
        double interval = textInterval.get();

        // 清空之前的文字，只保留最新的一组
        renderTexts.clear();

        // 确定标题文本
        //String titleText = isSlowingDown(crystal) ? "Shifting gears" : "Crystal Boosting";
        String titleText;

        titleText = "Crystal Boosting";
        if (isSlowingDown(crystal)) {
            titleText = "Smart Check: Slowed";
        } else if (crystalDamage >= BreakDamage.get()) {
            titleText = "Smart Check: Pause";
        }

        // 添加文字渲染
        renderTexts.add(new RenderText(targetPos.add(0, 1.5 + RenderYOffset.get() + textYOffset.get() - interval, 0), titleText));
        renderTexts.add(new RenderText(targetPos.add(0, 1.5 + RenderYOffset.get() + textYOffset.get(), 0), "Remaining Ticks: " + remainingTicks));
        renderTexts.add(new RenderText(targetPos.add(0, 1.5 + RenderYOffset.get() + textYOffset.get() + interval, 0), "Attack Count: 0/" + attackCount.get()));
        renderTexts.add(new RenderText(targetPos.add(0, 1.5 + RenderYOffset.get() + textYOffset.get() + interval * 2, 0), "Crystal Damage: " + String.format("%.1f", crystalDamage)));
        renderTexts.add(new RenderText(targetPos.add(0, 1.5 + RenderYOffset.get() + textYOffset.get() + interval * 3, 0), String.format("Distance: %.2fm", distance)));
        // 如果是智能减速模式，添加减速信息
        if (isSlowingDown(crystal)) {
            renderTexts.add(new RenderText(targetPos.add(0, 1.5 + RenderYOffset.get() + textYOffset.get() + interval * 4, 0), "Smart Speed: Slowed"));
        }

    }

    /**
     * 渲染方块
     */
    private void renderBlocks(Render3DEvent event) {
        if (RenderBlock.get() == false) {
            return;
        }
        for (RenderBlock block : renderBlocks) {
            // 计算透明度
            float alpha = (float) block.life / 40;

            // 确保透明度在0-1之间
            alpha = Math.max(0, Math.min(1, alpha));

            // 创建颜色（使用设置的颜色并应用透明度）
            Color side = new Color(
                sideColor.get().r,
                sideColor.get().g,
                sideColor.get().b,
                (int) (sideColor.get().a * alpha)
            );

            Color line = new Color(
                lineColor.get().r,
                lineColor.get().g,
                lineColor.get().b,
                (int) (lineColor.get().a * alpha)
            );

            // 渲染方块
            event.renderer.box(block.pos.add(0, RenderYOffset.get(), 0), side, line, shapeMode.get(), 0);
        }
    }

    /**
     * 渲染2D文字
     */
    private void renderText2D() {
        if (!renderText.get() || mc.player == null) return;

        // 渲染所有文字
        for (RenderText renderText : renderTexts) {
            renderSingleText(renderText);
        }

        // 同时渲染当前目标水晶的实时信息
        if (targetCrystal != null) {
            renderCurrentCrystalInfo();
        }
    }

    /**
     * 渲染单个文字信息
     */
    private void renderSingleText(RenderText renderText) {
        // 计算透明度
        float alpha = (float) renderText.life / RenderText.MAX_LIFE;
        alpha = Math.max(0, Math.min(1, alpha));

        // 创建颜色（使用设置的颜色并应用透明度）
        SettingColor color = new SettingColor(
            textColor.get().r,
            textColor.get().g,
            textColor.get().b,
            (int) (textColor.get().a * alpha)
        );

        // 转换Vec3d为Vector3d
        Vector3d vector3d = new Vector3d(renderText.pos.x, renderText.pos.y, renderText.pos.z);

        // 转换3D位置到2D屏幕坐标
        if (NametagUtils.to2D(vector3d, 1.0)) {
            NametagUtils.begin(vector3d);
            TextRenderer.get().begin(1, false, true);

            // 计算文字宽度并居中
            double textWidth = TextRenderer.get().getWidth(renderText.text) / 2.0 * fontScale.get();
            TextRenderer.get().render(renderText.text, -textWidth, 0, color);

            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    /**
     * 渲染当前水晶的实时信息
     */
    private void renderCurrentCrystalInfo() {
        if (targetCrystal == null) return;

        // 计算当前攻击速度
        int currentAttackSpeed = getCurrentAttackSpeed(targetCrystal);

        // 计算剩余tick
        int remainingTicks = currentAttackSpeed - (tickTimer % currentAttackSpeed);
        if (remainingTicks == currentAttackSpeed) {
            remainingTicks = 0;
        }

        // 计算水晶伤害
        float crystalDamage = 0;
        if (mc.player != null) {
            crystalDamage = crystalDamage(mc.player, targetCrystal.getPos());
        }

        // 获取水晶位置
        Vec3d targetPos = targetCrystal.getPos();
        double distance = mc.player.getPos().distanceTo(targetPos);

        // 计算文字间隔
        double interval = textInterval.get();

        // 确定标题文本
        String titleText = isSlowingDown(targetCrystal) ? "Shifting gears" : "Crystal Boosting";

        // 渲染标题
        renderRealTimeText(targetPos, 1.5 + RenderYOffset.get() + textYOffset.get() - interval, titleText);

        // 渲染剩余tick
        renderRealTimeText(targetPos, 1.5 + RenderYOffset.get() + textYOffset.get(), "Remaining Ticks: " + remainingTicks);

        // 渲染攻击次数
        renderRealTimeText(targetPos, 1.5 + RenderYOffset.get() + textYOffset.get() + interval, "Attack Count: " + currentAttackCount + "/" + attackCount.get());

        // 渲染水晶伤害
        renderRealTimeText(targetPos, 1.5 + RenderYOffset.get() + textYOffset.get() + interval * 2, "Crystal Damage: " + String.format("%.1f", crystalDamage));

        // 渲染距离
        renderRealTimeText(targetPos, 1.5 + RenderYOffset.get() + textYOffset.get() + interval * 3, String.format("Distance: %.2fm", distance));

        // 如果是智能减速模式，渲染减速信息
        if (isSlowingDown(targetCrystal)) {
            renderRealTimeText(targetPos, 1.5 + RenderYOffset.get() + textYOffset.get() + interval * 4, "Smart Speed: Slowed");
        }
    }

    /**
     * 渲染实时文字信息
     */
    private void renderRealTimeText(Vec3d pos, double yOffset, String text) {
        // 计算文字渲染位置
        Vec3d textRenderPos = pos.add(0, yOffset, 0);

        // 转换Vec3d为Vector3d
        Vector3d vector3d = new Vector3d(textRenderPos.x, textRenderPos.y, textRenderPos.z);

        // 转换3D位置到2D屏幕坐标
        if (NametagUtils.to2D(vector3d, 1.0)) {
            NametagUtils.begin(vector3d);
            TextRenderer.get().begin(1, false, true);

            // 计算文字宽度并居中
            double textWidth = TextRenderer.get().getWidth(text) / 2.0 * fontScale.get();
            TextRenderer.get().render(text, -textWidth, 0, textColor.get());

            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    /**
     * 查找范围内的末地水晶
     * @return 找到的末地水晶，未找到则返回null
     */
    private EndCrystalEntity findCrystal() {
        if (mc.world == null || mc.player == null) {
            return null;
        }

        EndCrystalEntity closestCrystal = null;
        double closestDistance = range.get();

        // 遍历所有实体，寻找末地水晶
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity) {
                double distance = mc.player.distanceTo(entity);
                // 检查是否在安全范围内，如果是则跳过
                if (distance <= safeRange.get()) {
                    continue;
                }
                if (distance <= range.get() && distance < closestDistance) {
                    closestCrystal = (EndCrystalEntity) entity;
                    closestDistance = distance;
                }
            }
        }

        return closestCrystal;
    }

    /**
     * 获取当前攻击速度
     * @param crystal 目标水晶
     * @return 调整后的攻击速度
     */
    private int getCurrentAttackSpeed(EndCrystalEntity crystal) {
        if (!smartSpeed.get() || crystal == null || mc.player == null) {
            return attackSpeed.get();
        }

        double distance = mc.player.distanceTo(crystal);
        if (distance <= speedThreshold.get()) {
            // 近距离时放慢速度
            return (int) Math.round(attackSpeed.get() * slowFactor.get());
        }
        return attackSpeed.get();
    }

    /**
     * 检查是否需要减速
     * @param crystal 目标水晶
     * @return 是否需要减速
     */
    private boolean isSlowingDown(EndCrystalEntity crystal) {
        if (!smartSpeed.get() || crystal == null || mc.player == null) {
            return false;
        }

        double distance = mc.player.distanceTo(crystal);
        return distance <= speedThreshold.get();
    }

    /**
     * 调试输出的信息
     * @param text 要输出的调试信息文本
     */
    private void OutputDebugInfo(String text) {
        if (debug.get() == true) {
            msg("[Debug] " + text);
        }
    }
    /**
     * 攻击末地水晶
     * @param crystal 要攻击的末地水晶
     */
    private void attackCrystal(EndCrystalEntity crystal) {
        FindItemResult CrystalResult;
        FindItemResult InvCrystalResult = null;
        boolean InvSwap = false;
        if (mc.player == null || mc.interactionManager == null) {
            return;
        }
        if (debug.get()) {
            msg("Attacking crystal " + attackCount.get() + " times");
        }

        // 重置攻击次数计数器
        currentAttackCount = 0;

        // 检测水晶伤害
        float crystalDamage_calc = 0.0f;
        float crystalDamage_toTarget = 0.0f;

        // 添加渲染方块
        BlockPos pos = crystal.getBlockPos();
        PlayerEntity target = null;
        boolean isFriend;
        renderBlocks.add(new RenderBlock(pos));
        //计算水晶的伤害
        crystalDamage_calc = crystalDamage(mc.player, crystal.getPos());

        // 按照设定的次数攻击水晶
        if(DamageCheck.get() && crystalDamage_calc >= BreakDamage.get()) {
            // 只要超过计算的数值就会直接取消操作 并且输出调试信息
            OutputDebugInfo("Crystal damage is " + crystalDamage_calc + " which is greater than " + BreakDamage.get());
            return;
        }
        currentActionEntity = crystal;
        for (int i = 1;i <= attackCount.get();i++) {
            mc.interactionManager.attackEntity(mc.player, crystal);
        }
        if (feetHelper.get() == true) { //判断塞脚辅助器是否开启
            if (feetHelper_rangeCheck.get() == true) { //检查是否为目标攻击模式
                target = TargetUtils.getPlayerTarget(feetHelper_range.get(), targetPriority.get()); //获取目标玩家 参数：范围, 搜索权重
                if (target == null) {
                    OutputDebugInfo("Not found target!");
                    return;
                } else {
                    crystalDamage_toTarget = crystalDamage(target, crystal.getPos());
                    OutputDebugInfo("CrystalDamage to target = " + crystalDamage_toTarget);
                    if (crystalDamage_toTarget <= targetBreakDamage.get()) return;
                    OutputDebugInfo("Found target! Name = " + target.getName() + " | Pos = " + target.getPos().toString() + " | ID = " + target.getId());
                }
                CrystalResult = InvUtils.findInHotbar(Items.END_CRYSTAL);
                if (CrystalResult.slot() == -1) {
                    InvCrystalResult = InvUtils.find(Items.END_CRYSTAL);
                    if (InvCrystalResult.slot() == -1) return;
                    OutputDebugInfo("开始进行InvSwap! 共5步！");
                    InvUtils.move().from(InvCrystalResult.slot()).to(1);
                    OutputDebugInfo("已将末地水晶交换到物品栏中，现在要放置水晶了 Step 1");
                    CrystalResult = InvUtils.findInHotbar(Items.END_CRYSTAL);
                    InvSwap = true;
                }
                for (int i = 0; i < PreCrystalPlace.get(); i++) {
                    try {
                        BlockUtils.place(crystal.getBlockPos(), CrystalResult, false, 1, false); // 放水晶
                    } catch (NullPointerException e) {
                        OutputDebugInfo(e.getMessage());
                    }

                }
                OutputDebugInfo("水晶放完了 现在开始将物品放回背包中 Step 2");
                if (InvSwap == true) {
                    CrystalResult = InvUtils.findInHotbar(Items.END_CRYSTAL);
                    OutputDebugInfo("已检测到物品 Index = " + CrystalResult.slot() + " Step 3");
                    if (InvCrystalResult == null) {
                        OutputDebugInfo("检测到数据错误 取消返回操作！ Step 4");
                        return;
                    }
                    InvUtils.move().from(CrystalResult.slot()).to(InvCrystalResult.slot());
                    OutputDebugInfo("成功将物品返回到物品栏中! From = " + CrystalResult.slot() + " | To = " + InvCrystalResult.slot() + "Step 5");
                }
            }
        }
    }
}
