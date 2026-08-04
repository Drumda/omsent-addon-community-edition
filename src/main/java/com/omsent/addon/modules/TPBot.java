package com.omsent.addon.modules;

import com.omsent.addon.AddonTemplate;
import com.omsent.addon.Utils.TPUtils;
import com.omsent.addon.Utils.PredictUtils;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
//迷你世界韩家辉大帝到此一游
public class TPBot extends Module {
    private static final com.omsent.addon.modules.TPBot INSTANCE = new com.omsent.addon.modules.TPBot();
    public static com.omsent.addon.modules.TPBot getInstance() { return INSTANCE; }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> toggleX = sgGeneral.add(new BoolSetting.Builder()
        .name("x")
        .description("Toggle X")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> toggleY = sgGeneral.add(new BoolSetting.Builder()
        .name("y")
        .description("Toggle Y")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> toggleZ = sgGeneral.add(new BoolSetting.Builder()
        .name("z")
        .description("Toggle Z")
        .defaultValue(true)
        .build()
    );
    private final Setting<Double> relX = sgGeneral.add(new DoubleSetting.Builder()
        .name("relative-x")
        .description("Relative target")
        .sliderRange(-25, 25)
        .defaultValue(0.0)
        .visible(toggleX::get)
        .build()
    );
    private final Setting<Double> relY = sgGeneral.add(new DoubleSetting.Builder()
        .name("relative-y")
        .description("Relative target")
        .sliderRange(-25, 25)
        .defaultValue(0.0)
        .visible(toggleY::get)
        .build()
    );
    private final Setting<Double> relZ = sgGeneral.add(new DoubleSetting.Builder()
        .name("relative-z")
        .description("Relative target")
        .sliderRange(-25, 25)
        .defaultValue(0.0)
        .visible(toggleZ::get)
        .build()
    );
    private final Setting<Boolean> randTp = sgGeneral.add(new BoolSetting.Builder()
        .name("random-tp")
        .description("In relative random teleport")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> usePredict = sgGeneral.add(new BoolSetting.Builder()
        .name("predict")
        .description("Predict target movement")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> ignF = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignore friends")
        .defaultValue(true)
        .build()
    );

    private Entity target;
    private PredictUtils predict = new PredictUtils();
    public TPBot() {
        super(AddonTemplate.CATEGORY,"Tp-bot", "Let you teleport to other players, but target is random, except teleport your friends, but you can setting it.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Main.enable) {
            toggle();
            return;
        }
        if (target == null || (target instanceof LivingEntity && ((LivingEntity) target).isDead()) || !target.isAlive()) {
            updateTarget();
            return;
        }
        predict.push(target.getPos());
        Vec3d tPos = predict.compute(target);
        if (!usePredict.get()) tPos = target.getPos();
        if (tPos == null || mc.player == null) return;
        double rx = relX.get();
        double ry = relY.get();
        double rz = relZ.get();
        double x;
        double y;
        double z;
        if (randTp.get()) {
            if (toggleX.get()) rx *= Math.random() * 2 - 1;
            if (toggleY.get()) ry *= Math.random() * 2 - 1;
            if (toggleZ.get()) rz *= Math.random() * 2 - 1;
        }
        if (!toggleX.get()) x = mc.player.getX();
        else x = tPos.x + rx;
        if (!toggleY.get()) y = mc.player.getY();
        else y = tPos.y + ry;
        if (!toggleZ.get()) z = mc.player.getZ();
        else z = tPos.z + rz;
        Vec3d newP = new Vec3d(x, y, z);
        mc.player.updatePosition(newP.x, newP.y, newP.z);
    }

    private void updateTarget() {
        target = TargetUtils.get(entity -> {
            if (entity == mc.player || entity.getType() != EntityType.PLAYER) return false;
            if ((entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) || !entity.isAlive()) return false;
            if (ignF.get() && entity instanceof PlayerEntity) {
                return Friends.get().shouldAttack((PlayerEntity) entity);
            }
            return true;
        }, SortPriority.LowestDistance);
    }

    @Override
    public void onActivate() {
        if (!Main.enable) {
            toggle();
            return;
        }
        updateTarget();
        predict.reset();
    }

    @Override
    public void onDeactivate() {
        target = null;
        if (mc.player != null) {
            TPUtils.moveP(mc.player.getPos());
        }
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }
}
