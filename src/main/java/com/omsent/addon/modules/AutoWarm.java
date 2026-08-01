package com.omsent.addon.modules;

import com.omsent.addon.NModule;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ArmorItem;
import net.minecraft.sound.SoundEvents;

public class AutoWarm extends NModule {
    private static final AutoWarm INSTANCE = new AutoWarm();
    public static AutoWarm getInstance() { return INSTANCE; }

    public enum ElytraDisplayMode {
        Constant,
        Blink
    }

    private final SettingGroup sgHealth = settings.createGroup("Health");
    private final SettingGroup sgArmor = settings.createGroup("Armor");
    private final SettingGroup sgElytra = settings.createGroup("Elytra");
    private final SettingGroup sgSound = settings.createGroup("Sound");

    // General settings

    // Sound settings
    private final Setting<Boolean> soundEnabled = sgSound.add(new BoolSetting.Builder()
        .name("sound-enabled")
        .description("Enable warning sound")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> soundInterval = sgSound.add(new IntSetting.Builder()
        .name("sound-interval")
        .description("Sound interval in ticks")
        .defaultValue(15)
        .min(1)
        .max(100)
        .sliderRange(1, 60)
        .visible(soundEnabled::get)
        .build()
    );

    private int soundTimer = 0;

    // Health settings
    private final Setting<Boolean> healthWarn = sgHealth.add(new BoolSetting.Builder()
        .name("health-warn")
        .description("Warn when health is low")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> healthThreshold = sgHealth.add(new DoubleSetting.Builder()
        .name("health-threshold")
        .description("Health threshold to warn at")
        .defaultValue(10.0)
        .min(1.0)
        .max(20.0)
        .sliderRange(1.0, 20.0)
        .visible(healthWarn::get)
        .build()
    );

    private final Setting<Double> healthXOffset = sgHealth.add(new DoubleSetting.Builder()
        .name("health-x-offset")
        .description("X offset for health warning")
        .defaultValue(0.0)
        .min(-1000)
        .max(1000)
        .sliderRange(-1000, 1000)
        .visible(healthWarn::get)
        .build()
    );

    private final Setting<Double> healthYOffset = sgHealth.add(new DoubleSetting.Builder()
        .name("health-y-offset")
        .description("Y offset for health warning")
        .defaultValue(20.0)
        .min(-1000)
        .max(1000)
        .sliderRange(-1000, 1000)
        .visible(healthWarn::get)
        .build()
    );

    private final Setting<SettingColor> healthColor = sgHealth.add(new ColorSetting.Builder()
        .name("health-color")
        .description("Color of the health warning")
        .defaultValue(new Color(255, 0, 0, 255))
        .visible(healthWarn::get)
        .build()
    );

    private final Setting<Double> healthSize = sgHealth.add(new DoubleSetting.Builder()
        .name("health-size")
        .description("Size of the health warning")
        .defaultValue(1.0)
        .min(0.5)
        .max(3.0)
        .sliderRange(0.5, 3.0)
        .visible(healthWarn::get)
        .build()
    );

    private final Setting<Boolean> healthShadow = sgHealth.add(new BoolSetting.Builder()
        .name("health-shadow")
        .description("Enable shadow for health warning")
        .defaultValue(true)
        .visible(healthWarn::get)
        .build()
    );

    private final Setting<Boolean> healthBlink = sgHealth.add(new BoolSetting.Builder()
        .name("health-blink")
        .description("Enable blinking for health warning")
        .defaultValue(false)
        .visible(healthWarn::get)
        .build()
    );

    private final Setting<Double> healthBlinkSpeed = sgHealth.add(new DoubleSetting.Builder()
        .name("health-blink-speed")
        .description("Blinking speed for health warning")
        .defaultValue(1.0)
        .min(0.1)
        .max(5.0)
        .sliderRange(0.1, 5.0)
        .visible(healthBlink::get)
        .build()
    );

    // Armor settings
    private final Setting<Boolean> armorWarn = sgArmor.add(new BoolSetting.Builder()
        .name("armor-warn")
        .description("Warn when armor durability is low")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> armorXOffset = sgArmor.add(new DoubleSetting.Builder()
        .name("armor-x-offset")
        .description("X offset for armor warning")
        .defaultValue(0.0)
        .min(-1000)
        .max(1000)
        .sliderRange(-1000, 1000)
        .visible(armorWarn::get)
        .build()
    );

    private final Setting<Double> armorYOffset = sgArmor.add(new DoubleSetting.Builder()
        .name("armor-y-offset")
        .description("Y offset for armor warning")
        .defaultValue(40.0)
        .min(-1000)
        .max(1000)
        .sliderRange(-1000, 1000)
        .visible(armorWarn::get)
        .build()
    );

    private final Setting<SettingColor> armorColor = sgArmor.add(new ColorSetting.Builder()
        .name("armor-color")
        .description("Color of the armor warning")
        .defaultValue(new Color(255, 0, 0, 255))
        .visible(armorWarn::get)
        .build()
    );

    private final Setting<Double> armorSize = sgArmor.add(new DoubleSetting.Builder()
        .name("armor-size")
        .description("Size of the armor warning")
        .defaultValue(1.0)
        .min(0.5)
        .max(3.0)
        .sliderRange(0.5, 3.0)
        .visible(armorWarn::get)
        .build()
    );

    private final Setting<Boolean> armorShadow = sgArmor.add(new BoolSetting.Builder()
        .name("armor-shadow")
        .description("Enable shadow for armor warning")
        .defaultValue(true)
        .visible(armorWarn::get)
        .build()
    );

    private final Setting<Boolean> armorBlink = sgArmor.add(new BoolSetting.Builder()
        .name("armor-blink")
        .description("Enable blinking for armor warning")
        .defaultValue(false)
        .visible(armorWarn::get)
        .build()
    );

    private final Setting<Double> armorBlinkSpeed = sgArmor.add(new DoubleSetting.Builder()
        .name("armor-blink-speed")
        .description("Blinking speed for armor warning")
        .defaultValue(1.0)
        .min(0.1)
        .max(5.0)
        .sliderRange(0.1, 5.0)
        .visible(armorBlink::get)
        .build()
    );

    private final Setting<Double> helmetThreshold = sgArmor.add(new DoubleSetting.Builder()
        .name("helmet-threshold")
        .description("Helmet durability threshold to warn at")
        .defaultValue(20.0)
        .min(1.0)
        .max(100.0)
        .sliderRange(1.0, 100.0)
        .visible(armorWarn::get)
        .build()
    );

    private final Setting<Double> chestplateThreshold = sgArmor.add(new DoubleSetting.Builder()
        .name("chestplate-threshold")
        .description("Chestplate durability threshold to warn at")
        .defaultValue(20.0)
        .min(1.0)
        .max(100.0)
        .sliderRange(1.0, 100.0)
        .visible(armorWarn::get)
        .build()
    );

    private final Setting<Double> leggingsThreshold = sgArmor.add(new DoubleSetting.Builder()
        .name("leggings-threshold")
        .description("Leggings durability threshold to warn at")
        .defaultValue(20.0)
        .min(1.0)
        .max(100.0)
        .sliderRange(1.0, 100.0)
        .visible(armorWarn::get)
        .build()
    );

    private final Setting<Double> bootsThreshold = sgArmor.add(new DoubleSetting.Builder()
        .name("boots-threshold")
        .description("Boots durability threshold to warn at")
        .defaultValue(20.0)
        .min(1.0)
        .max(100.0)
        .sliderRange(1.0, 100.0)
        .visible(armorWarn::get)
        .build()
    );
    private final Setting<Boolean> elytraWarn = sgElytra.add(new BoolSetting.Builder()
        .name("elytra-warn")
        .description("Warn when elytra durability is low")
        .defaultValue(true)
        .build()
    );
    private final Setting<SettingColor> elytraColor = sgElytra.add(new ColorSetting.Builder()
        .name("elytra-color")
        .description("Color of the elytra warning")
        .defaultValue(new Color(255, 0, 0, 255))
        .visible(elytraWarn::get)
        .build()
    );
    private final Setting<Double> elytraSize = sgElytra.add(new DoubleSetting.Builder()
        .name("elytra-size")
        .description("Size of the elytra warning")
        .defaultValue(1.0)
        .min(0.5)
        .max(3.0)
        .sliderRange(0.5, 3.0)
        .visible(elytraWarn::get)
        .build()
    );
    private final Setting<Boolean> elytraShadow = sgElytra.add(new BoolSetting.Builder()
        .name("elytra-shadow")
        .description("Enable shadow for elytra warning")
        .defaultValue(true)
        .visible(elytraWarn::get)
        .build()
    );
    private final Setting<Integer> elytraXOffset = sgElytra.add(new IntSetting.Builder()
        .name("elytra-x-offset")
        .description("X offset for elytra warning")
        .defaultValue(0)
        .min(-1000)
        .max(1000)
        .sliderRange(-1000, 1000)
        .visible(elytraWarn::get)
        .build()
    );
    private final Setting<Integer> elytraYOffset = sgElytra.add(new IntSetting.Builder()
        .name("elytra-y-offset")
        .description("Y offset for elytra warning")
        .defaultValue(0)
        .min(-1000)
        .max(1000)
        .sliderRange(-1000, 1000)
        .visible(elytraWarn::get)
        .build()
    );
    private final Setting<Double> elytraThreshold = sgElytra.add(new DoubleSetting.Builder()
        .name("elytra-threshold")
        .description("Elytra durability threshold to warn at")
        .defaultValue(20.0)
        .min(1.0)
        .max(100.0)
        .sliderRange(1.0, 100.0)
        .visible(elytraWarn::get)
        .build()
    );

    private final Setting<ElytraDisplayMode> elytraDisplayMode = sgElytra.add(new EnumSetting.Builder<ElytraDisplayMode>()
        .name("elytra-display-mode")
        .description("Display mode for elytra warning")
        .defaultValue(ElytraDisplayMode.Constant)
        .visible(elytraWarn::get)
        .build()
    );

    private final Setting<Double> elytraBlinkSpeed = sgElytra.add(new DoubleSetting.Builder()
        .name("elytra-blink-speed")
        .description("Blinking speed for elytra warning")
        .defaultValue(1.0)
        .min(0.1)
        .max(5.0)
        .sliderRange(0.1, 5.0)
        .visible(() -> elytraWarn.get() && elytraDisplayMode.get() == ElytraDisplayMode.Blink)
        .build()
    );
    public AutoWarm() {
        super("AutoWarm", "Warns when health or armor or elytra durability is low");
    }
    @Override
    public void onActivate() {
        if (!Main.enable) {
            toggle();
            return;
        }
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!Main.enable) {
            toggle();
            return;
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.player == null) return;

        boolean showHealthWarn = false;
        boolean showArmorWarn = false;
        boolean showElytraWarn = false;

        // Check health
        if (healthWarn.get()) {
            double health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (health <= healthThreshold.get()) {
                showHealthWarn = true;
            }
        }

        // Check armor durability
        if (armorWarn.get()) {
            showArmorWarn = checkArmorDurability();
        }

        // Check elytra durability
        if (elytraWarn.get()) {
            showElytraWarn = checkElytraDurability();
        }

        // Play sound if any warning is active
        if (showHealthWarn || showArmorWarn || showElytraWarn) {
            playWarningSound();
        } else {
            soundTimer = 0;
        }

        // Render warnings
        if (showHealthWarn || showArmorWarn || showElytraWarn) {
            renderWarnings(showHealthWarn, showArmorWarn, showElytraWarn);
        }
    }

    private boolean checkElytraDurability() {
        PlayerEntity player = mc.player;
        if (player == null) return false;
        ItemStack elytra = player.getInventory().armor.get(2);
        if (!elytra.isEmpty() && elytra.getItem() instanceof ElytraItem) {
            double durabilityPercent = getDurabilityPercent(elytra);
            if (durabilityPercent <= elytraThreshold.get()) {
                return true;
            }
        }
        return false;
    }

    private boolean checkArmorDurability() {
        PlayerEntity player = mc.player;
        if (player == null) return false;

        // Check helmet
        ItemStack helmet = player.getInventory().armor.get(3);
        if (!helmet.isEmpty() && helmet.getItem() instanceof ArmorItem) {
            double durabilityPercent = getDurabilityPercent(helmet);
            if (durabilityPercent <= helmetThreshold.get()) {
                return true;
            }
        }

        // Check chestplate
        ItemStack chestplate = player.getInventory().armor.get(2);
        if (!chestplate.isEmpty() && chestplate.getItem() instanceof ArmorItem) {
            double durabilityPercent = getDurabilityPercent(chestplate);
            if (durabilityPercent <= chestplateThreshold.get()) {
                return true;
            }
        }

        // Check leggings
        ItemStack leggings = player.getInventory().armor.get(1);
        if (!leggings.isEmpty() && leggings.getItem() instanceof ArmorItem) {
            double durabilityPercent = getDurabilityPercent(leggings);
            if (durabilityPercent <= leggingsThreshold.get()) {
                return true;
            }
        }

        // Check boots
        ItemStack boots = player.getInventory().armor.get(0);
        if (!boots.isEmpty() && boots.getItem() instanceof ArmorItem) {
            double durabilityPercent = getDurabilityPercent(boots);
            if (durabilityPercent <= bootsThreshold.get()) {
                return true;
            }
        }

        return false;
    }

    private double getDurabilityPercent(ItemStack itemStack) {
        if (itemStack.isEmpty() || !itemStack.isDamageable()) return 100.0;
        int maxDamage = itemStack.getMaxDamage();
        int damage = itemStack.getDamage();
        return ((double)(maxDamage - damage) / maxDamage) * 100.0;
    }

    private void renderWarnings(boolean showHealthWarn, boolean showArmorWarn, boolean showElytraWarn) {
        // Calculate screen center for X position
        double screenWidth = mc.getWindow().getScaledWidth();
        double screenCenterX = screenWidth / 2.0;

        // Render health warning
        if (showHealthWarn) {
            String healthText = "Please restore your status!";
            Color healthColor = getHealthColor();
            double healthSize_W;
            healthSize_W = healthSize.get();
            double textWidth = TextRenderer.get().getWidth(healthText) / 2.0;
            double x = screenCenterX + healthXOffset.get();
            double y = healthYOffset.get();

            TextRenderer.get().begin(healthSize_W, false, healthShadow.get());
            TextRenderer.get().render(healthText, x - textWidth, y, healthColor, healthShadow.get());
            TextRenderer.get().end();
        }

        // Render armor warning
        if (showArmorWarn) {
            String armorText = "Please repair your armor!";
            Color armorColor = getArmorColor();
            double armorSize_W;
            armorSize_W = armorSize.get();
            double textWidth = TextRenderer.get().getWidth(armorText) / 2.0;
            double x = screenCenterX + armorXOffset.get();
            double y = armorYOffset.get();

            TextRenderer.get().begin(armorSize_W, false, armorShadow.get());
            TextRenderer.get().render(armorText, x - textWidth, y, armorColor, armorShadow.get());
            TextRenderer.get().end();
        }

        // Render elytra warning
        if (showElytraWarn) {
            String elytraText = "Please repair your elytra!";
            Color elytraColor_W = getElytraColor();
            double elytraSize_W;
            elytraSize_W = elytraSize.get();
            double textWidth = TextRenderer.get().getWidth(elytraText) / 2.0;
            double x = screenCenterX + elytraXOffset.get();
            double y = elytraYOffset.get();

            TextRenderer.get().begin(elytraSize_W, false, elytraShadow.get());
            TextRenderer.get().render(elytraText, x - textWidth, y, elytraColor_W, elytraShadow.get());
            TextRenderer.get().end();
        }
    }

    private Color getHealthColor() {
        if (!healthBlink.get()) {
            return healthColor.get();
        }

        // Calculate blinking alpha using sine wave for smooth transition
        double time = System.currentTimeMillis() / 1000.0;
        double speed = healthBlinkSpeed.get();
        double alpha = (Math.sin(time * speed * Math.PI * 2) + 1) / 2; // Range 0-1

        Color baseColor = healthColor.get();
        return new Color(baseColor.r, baseColor.g, baseColor.b, (int)(baseColor.a * alpha));
    }

    private Color getArmorColor() {
        if (!armorBlink.get()) {
            return armorColor.get();
        }

        // Calculate blinking alpha using sine wave for smooth transition
        double time = System.currentTimeMillis() / 1000.0;
        double speed = armorBlinkSpeed.get();
        double alpha = (Math.sin(time * speed * Math.PI * 2) + 1) / 2; // Range 0-1

        Color baseColor = armorColor.get();
        return new Color(baseColor.r, baseColor.g, baseColor.b, (int)(baseColor.a * alpha));
    }

    private Color getElytraColor() {
        if (elytraDisplayMode.get() == ElytraDisplayMode.Constant) {
            return elytraColor.get();
        }

        // Blink mode
        // Calculate blinking alpha using sine wave for smooth transition
        double time = System.currentTimeMillis() / 1000.0;
        double speed = elytraBlinkSpeed.get();
        double alpha = (Math.sin(time * speed * Math.PI * 2) + 1) / 2; // Range 0-1

        Color baseColor = elytraColor.get();
        return new Color(baseColor.r, baseColor.g, baseColor.b, (int)(baseColor.a * alpha));
    }

    private void playWarningSound() {
        if (!soundEnabled.get() || mc.player == null) return;

        soundTimer++;
        if (soundTimer >= soundInterval.get()) {
            mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            soundTimer = 0;
        }
    }
}
