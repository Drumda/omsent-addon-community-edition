package com.omsent.addon.modules;


import com.omsent.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import net.minecraft.util.math.Vec3d;

public class AutoUseTimer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> multiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("multiplier")
        .description("Setting for timer")
        .defaultValue(1)
        .min(0.1)
        .sliderMin(0.1)
        .build()
    );
    private final Setting<Integer> CheckRange = sgGeneral.add(new IntSetting.Builder()
        .name("Check-range")
        .description("Set effective distance")
        .defaultValue(7)
        .min(1)
        .sliderMin(1)
        .max(25)
        .sliderMax(25)
        .build()
    );
    private final Setting<Boolean> IgnoreFriend = sgGeneral.add(new BoolSetting.Builder()
        .name("Ignore friends")
        .description("You don't know this setting? When you ask me, I just answer you: \"I don't know.\"")
        .defaultValue(true)
        .build()
    );
    public final Setting <Boolean> FriendTimer = sgGeneral.add(new BoolSetting.Builder()
        .name("when near friend open timer")
        .description("I don't know")
        .defaultValue(false)
        .build()
    );
    public final Setting<Double> FriendTimer_multiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("Friend timer multiplier")
        .description("")
        .defaultValue(1)
        .min(0.01)
        .visible(FriendTimer::get)
        .build()
    );
    public final Setting<Integer> FriendCheckRange = sgGeneral.add(new IntSetting.Builder()
        .name("Friend check range")
        .description("")
        .defaultValue(5)
        .min(1)
        .visible(FriendTimer::get)
        .build()
    );
    public final Setting<Timing> PlayerCheckTiming = sgGeneral.add(new EnumSetting.Builder<Timing>()
        .name("Player check timing")
        .description("timing")
        .defaultValue(Timing.Post)
        .build()
    );
    @Override
    public void onActivate() {
        if (!Main.enable) {
            toggle();
            return;
        }
    }
    public static final double OFF = 1;
    public AutoUseTimer() {
        super(AddonTemplate.CATEGORY, "AutoUseTimer", "Automatically start the timer when a player approaches you. By omsent");
    }
    public double getMultiplier() {
        return isActive() ? multiplier.get() : OFF;
    }
    public double getMultiplier_Friend() {
        return isActive() ? FriendTimer_multiplier.get() : OFF;
    }
    public boolean checkplayer() {
        if (mc.world == null) return false;
        if (mc.player == null) return false;
        Entity target = TargetUtils.get(entity -> {
            Vec3d pos = null;
            if (mc.player != null) {
                pos = mc.player.getPos();
            }
            Vec3d tpos = entity.getPos();
            if (entity == mc.player || entity.getType() != EntityType.PLAYER) return false;
            if (entity == mc.player && Friends.get().isFriend(mc.player) && IgnoreFriend.get()) return false;
            if ((entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) || !entity.isAlive()) return false;
            if (entity instanceof PlayerEntity) {
                if (!Friends.get().shouldAttack((PlayerEntity) entity)) return false;
            }
            return pos == null || !(Utils.distance(pos.x, pos.y, pos.z, tpos.x, tpos.y, tpos.z) > CheckRange.get());
        }, SortPriority.LowestDistance);
        return target != null;
    }
    public boolean checkplayer_forfriend() {
        if (mc.world == null) return false;
        if (mc.player == null) return false;
        Entity target = TargetUtils.get(entity -> {
            Vec3d pos = null;
            if (mc.player != null) {
                pos = mc.player.getPos();
            }
            Vec3d tpos = entity.getPos();
            if (entity == mc.player || entity.getType() != EntityType.PLAYER || !Friends.get().isFriend(mc.player)) return false;
            if (entity.getType() == EntityType.PLAYER && Friends.get().isFriend(mc.player)) return true;
            if ((entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) || !entity.isAlive()) return false;
            if (entity instanceof PlayerEntity) {
                if (!Friends.get().shouldAttack((PlayerEntity) entity)) return false;
            }
            return pos == null || !(Utils.distance(pos.x, pos.y, pos.z, tpos.x, tpos.y, tpos.z) > FriendCheckRange.get());
        }, SortPriority.LowestDistance);
        return target != null;
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!Main.enable) {
            toggle();
            return;
        }
        if(FriendTimer.get() == false && PlayerCheckTiming.get() == Timing.Post) {
            checkplayer();
        } else if (FriendTimer.get() == true && PlayerCheckTiming.get() == Timing.Post){
            checkplayer_forfriend();
        }
    }
    private void onTick(TickEvent.Pre event) {
        if(FriendTimer.get() == false && PlayerCheckTiming.get() == Timing.Pre) {
            checkplayer();
        } else if (FriendTimer.get() == true && PlayerCheckTiming.get() == Timing.Pre){
            checkplayer_forfriend();
        }
    }
    private enum Timing {
        Pre,
        Post
    }
}
