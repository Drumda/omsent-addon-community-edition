package com.omsent.addon.modules;

import com.omsent.addon.NModule;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.utils.world.BlockUtils;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AutoSwingGate extends NModule {
    private static final AutoSwingGate INSTANCE = new AutoSwingGate();
    public static AutoSwingGate getInstance() { return INSTANCE; }

    private final SettingGroup sgGeneral = settings.createGroup("General");

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("Target range")
        .defaultValue(5)
        .min(1)
        .max(10)
        .sliderRange(1, 10)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotate to place trapdoor")
        .defaultValue(true)
        .build()
    );

    private final Setting<SwapMode> swapMode = sgGeneral.add(new EnumSetting.Builder<SwapMode>()
        .name("swap-mode")
        .description("Item swap mode")
        .defaultValue(SwapMode.SILENT)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Swap back to previous item after placing")
        .defaultValue(true)
        .build()
    );

    private final Setting<PlaceMode> placeMode = sgGeneral.add(new EnumSetting.Builder<PlaceMode>()
        .name("place-mode")
        .description("Where to place trapdoor")
        .defaultValue(PlaceMode.HEAD)
        .build()
    );

    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("air-place")
        .description("Enable air place (place without support)")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> predictMovement = sgGeneral.add(new BoolSetting.Builder()
        .name("predict-movement")
        .description("Predict target movement for placement")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between placements (ticks)")
        .defaultValue(5)
        .min(0)
        .max(20)
        .sliderRange(0, 20)
        .build()
    );

    private int cooldown = 0;

    public AutoSwingGate() {
        super("AutoSwingGate", "Automatically place trapdoor on target's head to make them crawl");
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

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        PlayerEntity target = findTarget();
        if (target == null) {
            msg("No target found");
            return;
        }

        FindItemResult trapdoor = findTrapdoor();
        if (!trapdoor.found()) {
            error("No trapdoor found in inventory!");
            return;
        }

        msg("Found trapdoor in slot: " + trapdoor.slot());

        List<BlockPos> trapdoorPositions = getTrapdoorPositions(target);
        if (trapdoorPositions.isEmpty()) {
            msg("No valid positions found");
            return;
        }

        int prevSlot = mc.player.getInventory().selectedSlot;

        for (BlockPos trapdoorPos : trapdoorPositions) {
            if (canPlaceTrapdoor(trapdoorPos)) {
                msg("Placing trapdoor at: " + trapdoorPos);
                placeTrapdoorAtPosition(trapdoor, trapdoorPos);
                break;
            }
        }

        if (swapBack.get() && prevSlot >= 0) {
            swapToSlot(prevSlot);
        }

        cooldown = delay.get();
    }

    private void placeTrapdoorAtPosition(FindItemResult trapdoor, BlockPos pos) {
        if (mc.interactionManager == null || mc.player == null) return;

        // 切换到活板门
        swapToSlot(trapdoor.slot());

        // 确保持有正确的物品
        if (mc.player.getMainHandStack().isEmpty()) {
            error("No item in main hand!");
            return;
        }

        // 旋转到目标位置
        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), () -> {
                placeBlock(pos);
            });
        } else {
            placeBlock(pos);
        }
    }

    private PlayerEntity findTarget() {
        if (mc.world == null || mc.player == null) return null;

        PlayerEntity closest = null;
        double closestDistance = range.get() + 1;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!player.isAlive()) continue;

            double distance = mc.player.squaredDistanceTo(player);
            if (distance < closestDistance * closestDistance) {
                closest = player;
                closestDistance = Math.sqrt(distance);
            }
        }

        return closest;
    }

    private List<BlockPos> getTrapdoorPositions(PlayerEntity target) {
        List<BlockPos> positions = new ArrayList<>();
        if (target == null) return positions;

        BlockPos targetPos = target.getBlockPos();

        // 根据移动预测调整位置
        if (predictMovement.get()) {
            Vec3d velocity = target.getVelocity();
            targetPos = targetPos.add((int)velocity.x, 0, (int)velocity.z);
        }

        switch (placeMode.get()) {
            case HEAD:
                // 头上放置（调整为刚好在头部位置）
                positions.add(targetPos.up(1));
                break;
            case FEET:
                // 脚下放置
                positions.add(targetPos);
                break;
            case PREDICT:
                // 预判位置放置
                Vec3d velocity = target.getVelocity();
                BlockPos predictedPos = targetPos.add((int)(velocity.x * 2), 0, (int)(velocity.z * 2));
                positions.add(predictedPos.up(1)); // 预判头上
                positions.add(predictedPos); // 预判脚下
                break;
            case ALL:
                // 全部位置放置
                positions.add(targetPos.up(1)); // 头上
                positions.add(targetPos); // 脚下
                positions.add(targetPos.north()); // 北侧
                positions.add(targetPos.south()); // 南侧
                positions.add(targetPos.east()); // 东侧
                positions.add(targetPos.west()); // 西侧
                break;
        }

        return positions;
    }

    private boolean canPlaceTrapdoor(BlockPos pos) {
        if (mc.world == null || mc.player == null) return false;

        // 检查位置是否可放置
        if (!mc.world.getBlockState(pos).isReplaceable()) return false;

        // 检查玩家是否可以到达该位置
        if (mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > range.get() * range.get()) return false;

        // 如果开启air place，不需要支撑面检查
        if (airPlace.get()) {
            return true;
        }

        // 检查是否有支撑面
        BlockPos[] supportPositions = {
            pos.down(), pos.north(), pos.south(), pos.east(), pos.west()
        };

        for (BlockPos supportPos : supportPositions) {
            if (!mc.world.getBlockState(supportPos).isAir()) {
                return true;
            }
        }

        return false;
    }

    private FindItemResult findTrapdoor() {
        if (swapMode.get() == SwapMode.FROM_INVENTORY) {
            // 从整个物品栏中查找所有种类的活板门
            return InvUtils.find(itemStack -> {
                Item item = itemStack.getItem();
                Block block = Block.getBlockFromItem(item);
                return block instanceof TrapdoorBlock;
            });
        } else {
            // 只从快捷栏中查找所有种类的活板门
            return InvUtils.findInHotbar(itemStack -> {
                Item item = itemStack.getItem();
                Block block = Block.getBlockFromItem(item);
                return block instanceof TrapdoorBlock;
            });
        }
    }

    private void swapToSlot(int slot) {
        if (mc.player == null) return;

        if (swapMode.get() == SwapMode.SILENT) {
            InvUtils.swap(slot, true);
        } else {
            mc.player.getInventory().selectedSlot = slot;
        }
    }

    private void placeBlock(BlockPos pos) {
        if (mc.player == null) return;

        // 使用 BlockUtils 放置活板门，支持 air place
        BlockUtils.place(
            pos,
            findTrapdoor(),
            1,
            true
        );
    }

    public enum SwapMode {
        SILENT("Silent"),
        FROM_INVENTORY("From Inventory"),
        NORMAL("Normal");

        private final String title;

        SwapMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public enum PlaceMode {
        HEAD("Head"),
        FEET("Feet"),
        PREDICT("Predict"),
        ALL("All");

        private final String title;

        PlaceMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
