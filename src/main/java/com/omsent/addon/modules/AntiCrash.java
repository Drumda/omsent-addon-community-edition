package com.omsent.addon.modules;

import com.omsent.addon.NModule;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.orbit.EventHandler;

import java.util.concurrent.atomic.AtomicInteger;

public class AntiCrash extends NModule {
    private static final AntiCrash INSTANCE = new AntiCrash();
    public static AntiCrash getInstance() { return INSTANCE; }

    private final SettingGroup sgGeneral = settings.createGroup("General");
    private final SettingGroup sgLogging = settings.createGroup("Logging");

    private final Setting<Boolean> enabled = sgGeneral.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Enable anti-crash protection")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> catchAll = sgGeneral.add(new BoolSetting.Builder()
        .name("catch-all")
        .description("Catch all exceptions including mod errors")
        .defaultValue(true)
        .visible(enabled::get)
        .build()
    );

    private final Setting<Boolean> catchRuntime = sgGeneral.add(new BoolSetting.Builder()
        .name("catch-runtime")
        .description("Catch runtime exceptions")
        .defaultValue(true)
        .visible(enabled::get)
        .build()
    );

    private final Setting<Boolean> catchErrors = sgGeneral.add(new BoolSetting.Builder()
        .name("catch-errors")
        .description("Catch errors (not just exceptions)")
        .defaultValue(true)
        .visible(enabled::get)
        .build()
    );

    private final Setting<Boolean> catchThreadDeath = sgGeneral.add(new BoolSetting.Builder()
        .name("catch-thread-death")
        .description("Catch ThreadDeath errors")
        .defaultValue(false)
        .visible(enabled::get)
        .build()
    );

    private final Setting<Boolean> logErrors = sgLogging.add(new BoolSetting.Builder()
        .name("log-errors")
        .description("Log caught errors to chat")
        .defaultValue(false)
        .visible(enabled::get)
        .build()
    );

    private final Setting<Boolean> verboseLogging = sgLogging.add(new BoolSetting.Builder()
        .name("verbose-logging")
        .description("Show full stack traces")
        .defaultValue(false)
        .visible(() -> enabled.get() && logErrors.get())
        .build()
    );

    private final Setting<Integer> maxLogRate = sgLogging.add(new IntSetting.Builder()
        .name("max-log-rate")
        .description("Maximum error logs per second")
        .defaultValue(5)
        .min(1)
        .max(100)
        .sliderRange(1, 20)
        .visible(() -> enabled.get() && logErrors.get())
        .build()
    );

    private final Setting<Boolean> showStats = sgLogging.add(new BoolSetting.Builder()
        .name("show-stats")
        .description("Show statistics of caught errors")
        .defaultValue(true)
        .visible(enabled::get)
        .build()
    );

    private final Setting<Boolean> showDetailedStats = sgLogging.add(new BoolSetting.Builder()
        .name("show-detailed-stats")
        .description("Show detailed statistics including exception types")
        .defaultValue(false)
        .visible(enabled::get)
        .build()
    );

    private Thread.UncaughtExceptionHandler defaultHandler;
    private AtomicInteger caughtCount = new AtomicInteger(0);
    private AtomicInteger lastSecondCount = new AtomicInteger(0);
    private long lastSecondTime = 0;

    private AtomicInteger exceptionCount = new AtomicInteger(0);
    private AtomicInteger runtimeExceptionCount = new AtomicInteger(0);
    private AtomicInteger errorCount = new AtomicInteger(0);
    private java.util.concurrent.ConcurrentHashMap<String, AtomicInteger> exceptionTypeCount = new java.util.concurrent.ConcurrentHashMap<>();

    public AntiCrash() {
        super("AntiCrash", "Prevents crashes by catching all errors");
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();

        WButton showStats = list.add(theme.button("Show Statistics")).expandX().widget();
        showStats.action = () -> {
            displayDetailedStats();
        };

        WButton clearStats = list.add(theme.button("Clear Statistics")).expandX().widget();
        clearStats.action = () -> {
            clearStats();
        };

        WButton testException = list.add(theme.button("Test Exception")).expandX().widget();
        testException.action = () -> {
            testException();
        };

        return list;
    }

    @Override
    public void onActivate() {
        if (!Main.enable) {
            toggle();
            return;
        }
        if (enabled.get()) {
            installExceptionHandler();
        }
    }

    @Override
    public void onDeactivate() {
        uninstallExceptionHandler();
    }

    private void installExceptionHandler() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (shouldCatch(throwable)) {
                handleCaughtException(thread, throwable);
            } else if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });
    }

    private void uninstallExceptionHandler() {
        if (defaultHandler != null) {
            Thread.setDefaultUncaughtExceptionHandler(defaultHandler);
            defaultHandler = null;
        }
    }

    private boolean shouldCatch(Throwable throwable) {
        if (!enabled.get()) return false;

        if (throwable instanceof InterruptedException) {
            return catchThreadDeath.get();
        }

        if (throwable instanceof Error) {
            return catchErrors.get();
        }

        if (throwable instanceof RuntimeException) {
            return catchRuntime.get();
        }

        return catchAll.get();
    }

    private void handleCaughtException(Thread thread, Throwable throwable) {
        caughtCount.incrementAndGet();

        String exceptionType = throwable.getClass().getSimpleName();
        exceptionTypeCount.computeIfAbsent(exceptionType, k -> new AtomicInteger(0)).incrementAndGet();

        if (throwable instanceof Error) {
            errorCount.incrementAndGet();
        } else if (throwable instanceof RuntimeException) {
            runtimeExceptionCount.incrementAndGet();
        } else {
            exceptionCount.incrementAndGet();
        }

        if (logErrors.get()) {
            rateLimitedLog(thread, throwable);
        }

        if (showStats.get()) {
            updateStats();
        }
    }

    private void rateLimitedLog(Thread thread, Throwable throwable) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastSecondTime >= 1000) {
            lastSecondTime = currentTime;
            lastSecondCount.set(0);
        }

        if (lastSecondCount.get() < maxLogRate.get()) {
            lastSecondCount.incrementAndGet();
            logException(thread, throwable);
        }
    }

    private void logException(Thread thread, Throwable throwable) {
        String message = "Caught " + throwable.getClass().getSimpleName() +
                       " in thread " + thread.getName();

        if (verboseLogging.get()) {
            message += ": " + throwable.getMessage();
        }

        msg(message);
    }

    private void updateStats() {
        int total = caughtCount.get();
        int exceptions = exceptionCount.get();
        int runtimeExceptions = runtimeExceptionCount.get();
        int errors = errorCount.get();

        if (showDetailedStats.get()) {
            msg("=== AntiCrash Statistics ===");
            msg("Total caught: " + total);
            msg("Exceptions: " + exceptions);
            msg("Runtime Exceptions: " + runtimeExceptions);
            msg("Errors: " + errors);

            if (!exceptionTypeCount.isEmpty()) {
                msg("\nException Types:");
                exceptionTypeCount.forEach((type, count) -> {
                    msg("  " + type + ": " + count.get());
                });
            }
            msg("=========================");
        } else {
            msg("Total errors caught: " + total);
        }
    }

    private void displayDetailedStats() {
        int total = caughtCount.get();
        int exceptions = exceptionCount.get();
        int runtimeExceptions = runtimeExceptionCount.get();
        int errors = errorCount.get();

        msg("=== AntiCrash Detailed Statistics ===");
        msg("Total caught: " + total);
        msg("Exceptions: " + exceptions);
        msg("Runtime Exceptions: " + runtimeExceptions);
        msg("Errors: " + errors);

        if (!exceptionTypeCount.isEmpty()) {
            msg("\nException Types:");
            exceptionTypeCount.forEach((type, count) -> {
                msg("  " + type + ": " + count.get());
            });
        }
        msg("===================================");
    }

    private void clearStats() {
        caughtCount.set(0);
        exceptionCount.set(0);
        runtimeExceptionCount.set(0);
        errorCount.set(0);
        exceptionTypeCount.clear();
        msg("AntiCrash statistics cleared!");
    }

    private void testException() {
        msg("Testing exception handling...");
        try {
            throw new RuntimeException("Test exception from AntiCrash");
        } catch (Throwable t) {
            if (shouldCatch(t)) {
                handleCaughtException(Thread.currentThread(), t);
            } else {
                throw t;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!Main.enable) {
            toggle();
            return;
        }
        if (showStats.get() && caughtCount.get() > 0) {
            int total = caughtCount.get();
            int exceptions = exceptionCount.get();
            int runtimeExceptions = runtimeExceptionCount.get();
            int errors = errorCount.get();
            msg("AntiCrash active - Total: " + total + " | Exceptions: " + exceptions + " | Runtime: " + runtimeExceptions + " | Errors: " + errors);
        }
    }

    public static void wrapRunnable(Runnable runnable) {
        if (INSTANCE.isActive()) {
            try {
                runnable.run();
            } catch (Throwable t) {
                if (INSTANCE.shouldCatch(t)) {
                    INSTANCE.handleCaughtException(Thread.currentThread(), t);
                } else {
                    throw t;
                }
            }
        } else {
            runnable.run();
        }
    }

    public static <T> T wrapCallable(java.util.concurrent.Callable<T> callable, T defaultValue) {
        if (INSTANCE.isActive()) {
            try {
                return callable.call();
            } catch (Throwable t) {
                if (INSTANCE.shouldCatch(t)) {
                    INSTANCE.handleCaughtException(Thread.currentThread(), t);
                    return defaultValue;
                } else {
                    throw new RuntimeException(t);
                }
            }
        } else {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
