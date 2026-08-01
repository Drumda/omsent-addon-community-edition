package com.omsent.addon.modules;

import com.omsent.addon.NModule;
import com.omsent.addon.Utils.PredictUtils;
import com.omsent.addon.Utils.NInvUtils;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Box;
import org.joml.Vector3d;

// Rotation mode enum
enum RotationMode {
    SilentRotate,
    AllRotate
}

// Swap mode enum
enum SwapMode {
    NormalSwap,
    InventorySilentSwap
}
public class Follower extends NModule {
    private static final Follower INSTANCE = new Follower();
    public static Follower getInstance() { return INSTANCE; }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMovement = settings.createGroup("Movement");
    private final SettingGroup sgAntiCheat = settings.createGroup("Anti-Cheat");
    private final SettingGroup sgItemSwap = settings.createGroup("Item Swap");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // Auto activation settings
    private final Setting<Boolean> autoActivateElytra = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-activate-elytra")
        .description("Auto activate Elytra")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoEquipElytra = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-equip-elytra")
        .description("Auto equip Elytra from inventory")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoUnequipElytra = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-unequip-elytra")
        .description("Auto unequip Elytra when module is deactivated")
        .defaultValue(true)
        .build()
    );

    // AntiCrash settings
    private final Setting<Boolean> antiCrash = sgAntiCheat.add(new BoolSetting.Builder()
        .name("anti-crash")
        .description("Prevent kinetic damage from elytra")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> maxSpeed = sgAntiCheat.add(new DoubleSetting.Builder()
        .name("max-speed")
        .description("Maximum speed to prevent crash damage")
        .defaultValue(5.0)
        .min(1.0)
        .max(50.0)
        .sliderRange(1.0, 50.0)
        .visible(antiCrash::get)
        .build()
    );

    private final Setting<Double> activateHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("activate-height")
        .description("Activate height")
        .defaultValue(5.0)
        .min(1.0)
        .max(20.0)
        .sliderRange(1.0, 20.0)
        .visible(autoActivateElytra::get)
        .build()
    );

    // Target settings
    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignore friends")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> maxDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-distance")
        .description("Max track distance")
        .defaultValue(100.0)
        .min(10.0)
        .max(1024.0)
        .sliderRange(10.0, 1024)
        .build()
    );

    // Rotation mode settings
    private final Setting<RotationMode> rotationMode = sgMovement.add(new EnumSetting.Builder<RotationMode>()
        .name("rotation-mode")
        .description("Rotation mode")
        .defaultValue(RotationMode.SilentRotate)
        .build()
    );

    // Movement settings
    private final Setting<Double> speed = sgMovement.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Movement speed")
        .defaultValue(0.8)
        .min(0.1)
        .max(65)
        .sliderRange(0.1, 100)
        .build()
    );

    private final Setting<Double> heightOffset = sgMovement.add(new DoubleSetting.Builder()
        .name("height-offset")
        .description("Height offset")
        .defaultValue(2.0)
        .min(-10.0)
        .max(10.0)
        .sliderRange(-10.0, 10.0)
        .build()
    );

    // Auto slowdown settings
    private final Setting<Boolean> autoSlowdown = sgMovement.add(new BoolSetting.Builder()
        .name("auto-slowdown")
        .description("Automatically slow down when near target")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> slowdownRadius = sgMovement.add(new DoubleSetting.Builder()
        .name("slowdown-radius")
        .description("Radius around target to start slowing down")
        .defaultValue(2.0)
        .min(0.5)
        .max(10.0)
        .sliderRange(0.5, 10.0)
        .visible(autoSlowdown::get)
        .build()
    );

    private final Setting<Double> slowdownSpeed = sgMovement.add(new DoubleSetting.Builder()
        .name("slowdown-speed")
        .description("Speed when near target")
        .defaultValue(0.5)
        .min(0.1)
        .max(5.0)
        .sliderRange(0.1, 10.0)
        .visible(autoSlowdown::get)
        .build()
    );

    private final Setting<Boolean> usePrediction = sgMovement.add(new BoolSetting.Builder()
        .name("Predict")
        .description("Use prediction")
        .defaultValue(true)
        .build()
    );

    // Anti-cheat settings
    private final Setting<Boolean> smoothMovement = sgAntiCheat.add(new BoolSetting.Builder()
        .name("smooth-movement")
        .description("Smooth movement (Anti Cheat)")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> maxAngleChange = sgAntiCheat.add(new DoubleSetting.Builder()
        .name("max-angle-change")
        .description("Max angle change")
        .defaultValue(130)
        .min(1.0)
        .max(180.0)
        .sliderRange(1.0, 180.0)
        .build()
    );

    private final Setting<Integer> rotationDelay = sgAntiCheat.add(new IntSetting.Builder()
        .name("rotation-delay")
        .description("Delay between rotation updates (ticks)")
        .defaultValue(0)
        .min(0)
        .max(20)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Integer> fireworkInterval = sgAntiCheat.add(new IntSetting.Builder()
        .name("firework-interval")
        .description("Firework interval (ticks)")
        .defaultValue(20)
        .min(5)
        .max(100)
        .sliderRange(5, 100)
        .build()
    );

    private final Setting<Boolean> randomizeTiming = sgAntiCheat.add(new BoolSetting.Builder()
        .name("randomize-timing")
        .description("Randomize timing")
        .defaultValue(true)
        .build()
    );

    // Item swap settings
    private final Setting<SwapMode> swapMode = sgItemSwap.add(new EnumSetting.Builder<SwapMode>()
        .name("swap-mode")
        .description("Mode for swapping items")
        .defaultValue(SwapMode.NormalSwap)
        .build()
    );

    private final Setting<Integer> fromSlot = sgItemSwap.add(new IntSetting.Builder()
        .name("from-slot")
        .description("Slot to swap from (0-8)")
        .defaultValue(0)
        .min(0)
        .max(8)
        .sliderRange(0, 8)
        .build()
    );

    private final Setting<Integer> toSlot = sgItemSwap.add(new IntSetting.Builder()
        .name("to-slot")
        .description("Slot to swap to (0-8)")
        .defaultValue(1)
        .min(0)
        .max(8)
        .sliderRange(0, 8)
        .build()
    );

    private final Setting<Boolean> swapBack = sgItemSwap.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Swap back to original slot after use")
        .defaultValue(true)
        .build()
    );

    // Render settings
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Render target information")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> boxColor = sgRender.add(new ColorSetting.Builder()
        .name("box-color")
        .description("Color of the target box")
        .defaultValue(new SettingColor(255, 255, 255, 150))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> fillColor = sgRender.add(new ColorSetting.Builder()
        .name("fill-color")
        .description("Color of the target box fill")
        .defaultValue(new SettingColor(255, 255, 255, 25))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Color of the line to target")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> textColor = sgRender.add(new ColorSetting.Builder()
        .name("text-color")
        .description("Color of the distance text")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .visible(render::get)
        .build()
    );
    private final Setting<Integer> FontYOffset = sgRender.add(new IntSetting.Builder()
        .name("FontYOffset")
        .description("Y offset of the font")
        .defaultValue(0)
        .min(-100)
        .max(100)
        .sliderRange(-100, 100)
        .visible(render::get)
        .build()
    );
    private final Setting<Integer> FontScale = sgRender.add(new IntSetting.Builder()
        .name("FontScale")
        .description("Scale of the font")
        .defaultValue(1)
        .min(1)
        .max(20)
        .sliderRange(1, 20)
        .visible(render::get)
        .build()
    );

    private Entity target;
    private PredictUtils predict = new PredictUtils();
    private int fireworkCooldown = 0;
    private int lastFireworkTick = 0;
    private int lastRotationTick = 0;
    private Vec3d lastVelocity = Vec3d.ZERO;
    private boolean hasSwapped = false;

    public Follower() {
        super("Follower", "Auto follow target");
    }

    @Override
    public void onActivate() {
            if (!Main.enable) {
                toggle();
                return;
            }
        target = null;
        fireworkCooldown = 0;
        lastFireworkTick = 0;
        lastRotationTick = 0;
        lastVelocity = Vec3d.ZERO;
        hasSwapped = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Main.enable) {
            toggle();
            return;
        }
        if (mc.player == null) return;

        // Auto equip elytra if needed
        if (autoEquipElytra.get() && !mc.player.getInventory().armor.get(2).getItem().equals(Items.ELYTRA)) {
            equipElytra();
        }

        // Auto activate elytra
        if (autoActivateElytra.get() && !mc.player.isFallFlying()) {
            // Check if player is wearing elytra
            if (!mc.player.getInventory().armor.get(2).getItem().equals(Items.ELYTRA)) {
                // Try to equip elytra if not already equipped
                equipElytra();
                return;
            }

            // Try to activate elytra
            if (mc.player.isOnGround()) {
                // On ground: jump first
                mc.player.jump();
                mc.player.setSprinting(true);
            } else if (mc.player.getVelocity().y < 0) {
                // In air and falling: send fly packet
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                mc.options.jumpKey.setPressed(true);
            }
            return;
        }

        // Check elytra status
        if (!mc.player.isFallFlying()) {
            error("You need to equip and activate Elytra first!");
            return;
        }

        // Update target
        if (target == null || !target.isAlive() || target.distanceTo(mc.player) > maxDistance.get()) {
            updateTarget();
            if (target == null) {
                // Only show error when no target found for the first time
                if (mc.player.age % 100 == 0) {
                    error("No valid target found!");
                }
                return;
            }
        }

        // Predict target position
        Vec3d targetPos = getTargetPosition();

        // Calculate movement direction
        Vec3d direction = calculateMovementDirection(targetPos);

        // Apply movement
        applyMovement(direction);

        // Manage firework usage
        manageFireworks();

        // Update cooldown
        if (fireworkCooldown > 0) fireworkCooldown--;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || mc.player == null || target == null || !target.isAlive()) return;

        // Get target position (use predicted position if prediction is enabled)
        Vec3d targetPos = getTargetPosition();
        Vec3d playerPos = mc.player.getPos();

        // Render box at target position
        double width = 0.6;
        double height = 1.8;
        Box box = new Box(
            targetPos.x - width / 2, targetPos.y - 1.5 - heightOffset.get(),
            targetPos.z - width / 2, targetPos.x + width / 2, targetPos.y - heightOffset.get(),
            targetPos.z + width / 2
        );

        event.renderer.box(box, fillColor.get(), boxColor.get(), ShapeMode.Both, 0);

        // Render line from player to target
        Vec3d playerCenter = playerPos.add(0, mc.player.getEyeHeight(mc.player.getPose()) / 2, 0);
        Vec3d targetCenter = targetPos;
        event.renderer.line(playerCenter.x, playerCenter.y, playerCenter.z, targetCenter.x, targetCenter.y, targetCenter.z, lineColor.get());
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!render.get() || mc.player == null || target == null || !target.isAlive()) return;
        // Get target position and calculate distance
        Vec3d targetPos = target.getPos();
        Vec3d textRenderPos = targetPos.add(0, target.getHeight() + 0.5, 0);
        double distance = mc.player.getPos().distanceTo(targetPos);

        // Convert Vec3d to Vector3d for NametagUtils
        Vector3d vector3d = new Vector3d(textRenderPos.x, textRenderPos.y, textRenderPos.z);

        // Convert 3D position to 2D screen coordinates
        if (NametagUtils.to2D(vector3d, 1.0)) {
            NametagUtils.begin(vector3d);
            TextRenderer.get().begin(1, false, true);

            // Render distance text
            String text = String.format("Distance: %.2f" + "m", distance);
            double textWidth = TextRenderer.get().getWidth(text) / 2 * FontScale.get();
            TextRenderer.get().render(text, -textWidth, FontYOffset.get(), textColor.get());
            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    private void updateTarget() {
        target = TargetUtils.get(entity -> {
            if (entity == mc.player || !(entity instanceof PlayerEntity)) return false;
            if (!entity.isAlive()) return false;
            if (entity.distanceTo(mc.player) > maxDistance.get()) return false;
            if (ignoreFriends.get() && Friends.get().isFriend((PlayerEntity) entity)) return false;
            return true;
        }, SortPriority.LowestDistance);
    }

    private Vec3d getTargetPosition() {
        if (usePrediction.get()) {
            predict.push(target.getPos());
            return predict.compute(target);
        }
        return target.getPos().add(0,1.5 + heightOffset.get(),0);
    }

    private Vec3d calculateMovementDirection(Vec3d targetPos) {
        Vec3d playerPos = mc.player.getPos();
        Vec3d rawDirection = targetPos.subtract(playerPos).normalize();

        if (smoothMovement.get()) {
            return smoothDirectionChange(rawDirection);
        }

        return rawDirection;
    }

    private Vec3d smoothDirectionChange(Vec3d targetDirection) {
        Vec3d currentLook = mc.player.getRotationVector();

        // Limit angle change
        double maxChange = Math.toRadians(maxAngleChange.get());

        // Calculate angle difference between current direction and target direction
        double angle = Math.acos(currentLook.dotProduct(targetDirection));

        if (angle <= maxChange) {
            return targetDirection;
        }

        // Interpolate smooth transition
        double t = maxChange / angle;
        return currentLook.lerp(targetDirection, t);
    }

    private void applyMovement(Vec3d direction) {
        // Calculate target angles
        float targetYaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
        float targetPitch = (float) Math.toDegrees(Math.asin(-direction.y));

        // Check if rotation delay has passed
        if (mc.player.age - lastRotationTick >= rotationDelay.get()) {
            // Apply rotation based on rotation mode
            switch (rotationMode.get()) {
                case AllRotate:
                    // All Rotate: Directly rotate player perspective
                    Rotations.rotate(targetYaw, targetPitch);
                    break;
                case SilentRotate:
                    // Silent Rotate: Client perspective doesn't rotate, but affects elytra direction
                    applySilentRotation(targetYaw, targetPitch, direction);
                    break;
            }
            lastRotationTick = mc.player.age;
        }

        // Calculate speed based on distance to target
        double currentSpeed = speed.get();
        if (autoSlowdown.get() && target != null) {
            double distanceToTarget = mc.player.getPos().distanceTo(target.getPos());
            if (distanceToTarget <= slowdownRadius.get()) {
                currentSpeed = slowdownSpeed.get();
            }
        }

        // Apply movement input with speed limit if anti-crash is enabled
        Vec3d newVelocity = direction.multiply(currentSpeed);

        // Limit speed if anti-crash is enabled
        if (antiCrash.get()) {
            double speedLength = newVelocity.length();
            if (speedLength > maxSpeed.get()) {
                newVelocity = newVelocity.normalize().multiply(maxSpeed.get());
            }
        }

        mc.player.setVelocity(newVelocity);

        // Record speed for smoothing
        lastVelocity = newVelocity;
    }

    private void applySilentRotation(float targetYaw, float targetPitch, Vec3d direction) {
        // Get current angles
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        // Smooth angle change
        float smoothedYaw = smoothAngleChange(currentYaw, targetYaw, maxAngleChange.get());
        float smoothedPitch = smoothAngleChange(currentPitch, targetPitch, maxAngleChange.get());

        // Directly set player angles (won't rotate client perspective)
        mc.player.setYaw(smoothedYaw);
        mc.player.setPitch(smoothedPitch);

        // Update server angles (affects elytra flight direction)
        mc.player.prevYaw = smoothedYaw;
        mc.player.prevPitch = smoothedPitch;
    }

    private float smoothAngleChange(float current, float target, Double maxChange) {
        float difference = MathHelper.wrapDegrees(target - current);

        if (Math.abs(difference) <= maxChange) {
            return target;
        }

        // Limit maximum change
        float change = (float) (Math.signum(difference) * maxChange);
        return current + change;
    }

    private void manageFireworks() {
        if (fireworkCooldown > 0) return;

        // Check if firework is needed
        boolean shouldUseFirework = shouldUseFirework();

        if (shouldUseFirework) {
            useFirework();

            // Set cooldown
            int interval = fireworkInterval.get();
            if (randomizeTiming.get()) {
                interval += (int) (Math.random() * 10) - 5; // ±5 ticks random variation
            }
            fireworkCooldown = Math.max(5, interval);
            lastFireworkTick = mc.player.age;
        }
    }

    private boolean shouldUseFirework() {
        // Check if speed is too low
        double currentSpeed = mc.player.getVelocity().length();
        if (currentSpeed < speed.get() * 0.5) {
            return true;
        }

        // Check if falling
        if (mc.player.getVelocity().y < -0.5) {
            return true;
        }

        // Use fireworks regularly to maintain flight
        int ticksSinceLastFirework = mc.player.age - lastFireworkTick;
        return ticksSinceLastFirework >= fireworkInterval.get();
    }

    private void useFirework() {
        FindItemResult firework = InvUtils.find(itemStack ->
            itemStack.getItem() == Items.FIREWORK_ROCKET);

        if (!firework.found()) {
            error("No firework rocket found!");
            error("Close the module.");
            toggle();
            return;
        }

        // Switch to firework
        swapToSlot(firework.slot());

        // Use firework
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);

        // Swap back if enabled
        if (swapBack.get()) {
            NInvUtils.swapBack();
        }
        hasSwapped = false;
    }

    private void equipElytra() {
        FindItemResult elytra = InvUtils.find(itemStack ->
            itemStack.getItem() == Items.ELYTRA);

        if (elytra.found()) {
            // Equip elytra to chest slot (index 2 in armor inventory)
            InvUtils.move().from(elytra.slot()).toArmor(2);
        }
    }

    private void swapToSlot(int slot) {
        int currentSlot = mc.player.getInventory().selectedSlot;

        if (currentSlot == slot) return;

        switch (swapMode.get()) {
            case NormalSwap:
                normalSwap(currentSlot, slot);
                break;
            case InventorySilentSwap:
                inventorySilentSwap(currentSlot, slot);
                break;
        }

        hasSwapped = true;
    }

    private void normalSwap(int from, int to) {
        if (swapBack.get()) {
            NInvUtils.swap(from, to, true);
        } else {
            InvUtils.move().from(from).to(to);
        }
    }

    private void inventorySilentSwap(int from, int to) {
        if (swapBack.get()) {
            NInvUtils.swap(from, to, true);
        } else {
            InvUtils.move().from(from).to(to);
        }
    }

    @Override
    public void onDeactivate() {
        if (swapBack.get()) {
            NInvUtils.swapBack();
        }

        // Auto unequip elytra if enabled
        if (autoUnequipElytra.get() && mc.player != null) {
            unequipElytra();
        }
        if (mc.player != null) {
            mc.player.setVelocity(0,0,0);
        }
        hasSwapped = false;
        if (mc.player != null) {
            mc.player.jump();
        }
    }

    private void unequipElytra() {
        // Check if player is wearing elytra
        if (mc.player.getInventory().armor.get(2).getItem().equals(Items.ELYTRA)) {
            // Try to find a chestplate first
            FindItemResult chestplate = InvUtils.find(itemStack ->
                itemStack.getItem() == Items.NETHERITE_CHESTPLATE ||
                itemStack.getItem() == Items.DIAMOND_CHESTPLATE ||
                itemStack.getItem() == Items.IRON_CHESTPLATE ||
                itemStack.getItem() == Items.GOLDEN_CHESTPLATE ||
                itemStack.getItem() == Items.CHAINMAIL_CHESTPLATE ||
                itemStack.getItem() == Items.LEATHER_CHESTPLATE
            );

            if (chestplate.found()) {
                // Swap elytra with chestplate directly
                // First, move elytra to chestplate's slot
                InvUtils.move().fromArmor(2).to(chestplate.slot());
                // Then, move chestplate to armor slot
                InvUtils.move().from(chestplate.slot()).toArmor(2);
            } else {
                // No chestplate found, just move elytra to any available slot
                // Find any slot (not necessarily empty)
                for (int i = 0; i < 36; i++) {
                    // Skip hotbar if possible to avoid messing with player's selected items
                    if (i < 9) continue;

                    // Try to move elytra to this slot
                    InvUtils.move().fromArmor(2).to(i);
                    break;
                }
            }
        }
    }

    @Override
    public String getInfoString() {
        if (target instanceof PlayerEntity) {
            return meteordevelopment.meteorclient.utils.entity.EntityUtils.getName(target);
        }
        return null;
    }
}
