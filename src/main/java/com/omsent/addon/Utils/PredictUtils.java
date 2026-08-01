package com.omsent.addon.Utils;

import com.omsent.addon.modules.Predict;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class PredictUtils {
    private final List<Vec3d> points = new ArrayList<>();
    private Vec3d currentPos;

    public Predict getPredict() {
        return Predict.getInstance();
    }
    private Predict.Mode getMode() {
        return getPredict().mode.get();
    }
    private Integer getLimit() {
        return getPredict().limit.get();
    }
    private Double getPower() {
        return getPredict().power.get();
    }
    private boolean getActive() {
        return getPredict().isActive();
    }

    public PredictUtils() {
        points.clear();
        currentPos = null;
    }
    public void reset() {
        points.clear();
        currentPos = null;
    }

    public void push(Vec3d v) {
        currentPos = v;
        points.add(v);
        if (points.size() > getLimit()) points.removeFirst();
    }

    public Vec3d compute(Entity entity) {
        if (entity != null && getMode() == Predict.Mode.Velocity) {
            return currentPos.add(entity.getVelocity().multiply(getPower()));
        }

        if (currentPos == null) return null;

        Vec3d result = null;
        if (!points.isEmpty()) result = currentPos;
        if (!getActive() || points.size() < 2 || result == null) return result;
        List<Vec3d> diff = new ArrayList<>();
        Vec3d oldV = null;
        for (Vec3d v : points) {
            if (oldV == null) {
                oldV = v;
                continue;
            }

            diff.add(v.subtract(oldV));

            oldV = v;
        }
        if (diff.size() >= 2) {
            Vec3d d = new Vec3d(0,0,0);
            for (Vec3d v : diff) {
                d = d.add(v).multiply(0.5);
            }
            return result.add(d.multiply(getPower()));
        } else if (diff.size() == 1) {
            return currentPos.add(diff.get(0).multiply(getPower()));
        }

        return result;
    }

    public Vec3d compute() {
        return compute(null);
    }
}
