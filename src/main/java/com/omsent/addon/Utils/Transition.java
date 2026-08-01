package com.omsent.addon.Utils;

import java.util.function.Consumer;
import java.util.function.Function;

public class Transition {
    public final double from;
    public final double to;
    private final double total;
    public final int steps;
    public final Function<Transition, Double> f;
    private Consumer<Double> action;
    private int step;
    private double progress;
    private boolean isOk;

    private double next() {
        if (isOk) return to;
        if (steps <= step || steps <= 0) {
            isOk = true;
            return to;
        }
        if (from == to) {
            isOk = true;
            return to;
        }
        progress = ((total / steps) * step) / total;
        step++;
        return from + f.apply(this) * total;
    }
    public boolean ok() {
        return isOk;
    }

    public void update() {
        action.accept(next());
    }

    public int getStep() {
        return this.step;
    }

    private Transition(double from, double to, int steps, Function<Transition, Double> transitionF, Consumer<Double> action) {
        this.from = from;
        this.to = to;
        this.steps = steps;
        this.step = 0;
        this.total = to - from;
        this.progress = 0.0;
        this.f = transitionF;
        this.action = action;
        this.isOk = false;
    }

    public static Transition create(double from, double to, int steps, Function<Transition, Double> transitionF, Consumer<Double> action) {
        return new Transition(from, to, steps, transitionF, action);
    }
    public static Transition create(double from, double to, int steps, Function<Transition, Double> transitionF) {
        return new Transition(from, to, steps, transitionF, p -> {});
    }
    public static Transition create(double from, double to, int steps, Consumer<Double> action) {
        return create(from, to, steps,
            Easings.easeOutExpo,
            action);
    }
    public static Transition create(Transition old, double to, Consumer<Double> action) {
        return create(old, old.to, to, action);
    }
    public static Transition create(Transition old, double from, double to, Consumer<Double> action) {
        old.action = action;
        if (old.to == to) return old;
        else return create(from, to, old.steps, old.f, action);
    }

    public static Transition create(int steps) {
        return create(steps, Easings.easeOutExpo);
    }

    public static Transition create(int steps, Function<Transition, Double> transitionF) {
        return create(0.0, 0.0, steps, transitionF, p -> {});
    }

    public static class Easings {
        public final static Function<Transition, Double> easeInExpo = t -> Math.pow(2,10 * t.progress - 10);
        public final static Function<Transition, Double> easeOutExpo = t -> 1.0 - Math.pow(2,-10 * t.progress);
    }
}
