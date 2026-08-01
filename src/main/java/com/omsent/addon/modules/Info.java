package com.omsent.addon.modules;

import com.omsent.addon.NModule;
import com.omsent.addon.Utils.TPUtils;
import com.omsent.addon.Utils.Transition;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Formatting;
import net.minecraft.network.packet.Packet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.text.*;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import java.util.UUID;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import org.joml.Vector3d;

public class Info extends NModule {
    private static final Info INSTANCE = new Info();
    public static Info getInstance() { return INSTANCE; }

    private final List<Vec3d> movePacketsPos = new ArrayList<>();
    private final List<Vec3d> lagPacketsPos = new ArrayList<>();
    private final List<Notify> notifies = new ArrayList<>();
    private final ItemStack fixBugStack = new ItemStack(Items.END_CRYSTAL, 64);
    private Entity combotEntity = null;
    private int passTick = 0;
    private Transition hudHealthTransi = Transition.create(5);
    private Transition hudHungerTransi = Transition.create(5);
    private Transition hudSatTransi = Transition.create(5);
    private Transition hudXPTransi = Transition.create(5);
    private Transition hudArmorTransi = Transition.create(5);
    private Transition hudSlotTransi = Transition.create(5);

    private final SettingGroup sgMovePacket = this.settings.createGroup("MovePacket");
    private final SettingGroup sgCombot = this.settings.createGroup("Combot");
    private final SettingGroup sgPacket = this.settings.createGroup("Packet Debug");
    private final SettingGroup sgHud = this.settings.createGroup("Hud");
    private final SettingGroup sgNotify = this.settings.createGroup("Nofity");
    private final SettingGroup sgDeathOf = this.settings.createGroup("Player Death Of");
    private final SettingGroup sgPJL = this.settings.createGroup("Player join/leave");

    // Move packet settings
    private final Setting<Boolean> toggleMovePacket = sgMovePacket.add(new BoolSetting.Builder()
            .name("toggle-move-packet")
            .description("Render move packet")
            .defaultValue(true)
            .onChanged(i -> movePacketsPos.clear())
            .build()
    );
    private final Setting<SettingColor> movePacketColor = sgMovePacket.add(new ColorSetting.Builder()
            .name("move-packet-color")
            .description("Set move packet render color")
            .defaultValue(new SettingColor(0,255,0,255))
            .build()
    );
    private final Setting<SettingColor> lagPacketColor = sgMovePacket.add(new ColorSetting.Builder()
            .name("lag-packet-color")
            .description("Set lag packet render color")
            .defaultValue(new SettingColor(255,0,0,255))
            .onChanged(i -> lagPacketsPos.clear())
            .build()
    );
    private final Setting<Integer> movePacketInterval = sgMovePacket.add(new IntSetting.Builder()
            .name("move-packet-interval")
            .description("Set move packet interval ticks")
            .min(0)
            .defaultValue(0)
            .sliderRange(0, 20)
            .build()
    );

    // Combot
    private final Setting<Boolean> toggleCombot = sgCombot.add(new BoolSetting.Builder()
            .name("toggle-combot")
            .description("Render combot entity")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> combotEntityColor = sgCombot.add(new ColorSetting.Builder()
            .name("combot-entity-color")
            .description("Render player fill color")
            .defaultValue(new SettingColor(255,0,255,76,true))
            .build()
    );

    // Packet Debug
    private final Setting<Boolean> togglePacket = sgPacket.add(new BoolSetting.Builder()
            .name("toggle-packet-debug")
            .description("Send packet message")
            .defaultValue(true)
            .build()
    );
    private final Setting<Set<Class<? extends Packet<?>>>> packetC2S = sgPacket.add(new PacketListSetting.Builder()
            .name("packet-debug-C2S")
            .description("Server-to-client packets to Send")
            .filter(aClass -> PacketUtils.getC2SPackets().contains(aClass))
            .build()
    );
    private final Setting<Set<Class<? extends Packet<?>>>> packetS2C = sgPacket.add(new PacketListSetting.Builder()
            .name("packet-debug-S2C")
            .description("Client-to-server packets to Send")
            .filter(aClass -> PacketUtils.getS2CPackets().contains(aClass))
            .build()
    );

    // Hud
    public final Setting<Boolean> toggleHud = sgHud.add(new BoolSetting.Builder()
            .name("toggle-hud")
            .description("Render hud")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> hudHotbarColor = sgHud.add(new ColorSetting.Builder()
            .name("hud-hotbar-color")
            .description("Hotbar background color")
            .defaultValue(new SettingColor(0,0,0,178))
            .build()
    );
    private final Setting<SettingColor> hudHotbarSlotColor = sgHud.add(new ColorSetting.Builder()
            .name("hud-hotbar-slot-color")
            .description("Hotbar selected slot color")
            .defaultValue(new SettingColor(255,50,255,150))
            .build()
    );

    // Toggle Notice later
    public final Setting<Boolean> toggleNotify = sgNotify.add(new BoolSetting.Builder()
            .name("toggle-notify")
            .description("Modules toggle notify")
            .defaultValue(true)
            .onChanged(i -> notifies.clear() )
            .build()
    );
    private final Setting<NotifyPosition> notifyPosition = sgNotify.add(new EnumSetting.Builder<NotifyPosition>()
            .name("notify-position")
            .description("Notify display position")
            .defaultValue(NotifyPosition.RightTop)
            .build()
    );
    private final Setting<Integer> notifyStayTime = sgNotify.add(new IntSetting.Builder()
            .name("notify-stay-time")
            .min(0)
            .sliderRange(0, 100)
            .defaultValue(4)
            .build()
    );
    private final Setting<Integer> notifyTransiSpeed = sgNotify.add(new IntSetting.Builder()
            .name("notify-transition-speed")
            .min(1)
            .sliderRange(1, 20)
            .defaultValue(3)
            .build()
    );
    private final Setting<Double> notifyWidth = sgNotify.add(new DoubleSetting.Builder()
            .name("notify-width")
            .min(0)
            .sliderRange(0, 400)
            .defaultValue(200)
            .build()
    );
    private final Setting<Double> notifyHeight = sgNotify.add(new DoubleSetting.Builder()
            .name("notify-height")
            .min(0)
            .sliderRange(0, 100)
            .defaultValue(50)
            .build()
    );
    private final Setting<SettingColor> notifyActiveColor = sgNotify.add(new ColorSetting.Builder()
            .name("notify-active-color")
            .defaultValue(new SettingColor(30,255,30,127))
            .build()
    );
    private final Setting<SettingColor> notifyDeactiveColor = sgNotify.add(new ColorSetting.Builder()
            .name("notify-deactive-color")
            .defaultValue(new SettingColor(178, 0, 0, 127))
            .build()
    );
    private final Setting<SettingColor> notifyActiveTextColor = sgNotify.add(new ColorSetting.Builder()
            .name("notify-active-text-color")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );
    private final Setting<SettingColor> notifyDeactiveTextColor = sgNotify.add(new ColorSetting.Builder()
            .name("notify-deactive-text-color")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );
    public final Setting<Boolean> notifyTextShadow = sgNotify.add(new BoolSetting.Builder()
            .name("notify-text-shadow")
            .defaultValue(false)
            .build()
    );

    // Player Death Of
    private final Setting<Boolean> toggleDeathOf = sgDeathOf.add(new BoolSetting.Builder()
            .name("toggle-player-death-of")
            .description("Player death of ...")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> deathOfNotify = sgDeathOf.add(new BoolSetting.Builder()
            .name("player-death-of-notify")
            .description("Use notify")
            .defaultValue(true)
            .build()
    );

    // Player join/leave
    private final Setting<Boolean> togglePJL = sgPJL.add(new BoolSetting.Builder()
            .name("toggle-player-join-leave")
            .description("How to handle player join/leave notifications.")
            .defaultValue(true)
            .build()
    );
    private final Setting<PJLMode> pjlMode = sgPJL.add(new EnumSetting.Builder<PJLMode>()
            .name("player-join-leave-mode")
            .description("How to send")
            .defaultValue(PJLMode.Both)
            .build()
    );

    public Info() {
        super("info", "i don't know how to use this module :( By osmuikosmh");
    }

    @Override
    public void onActivate() {

        passTick = 0;
        movePacketsPos.clear();
        lagPacketsPos.clear();
        notifies.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (passTick < movePacketInterval.get())
            passTick++;
        else {
            movePacketsPos.clear();
            lagPacketsPos.clear();
            passTick = 0;
        }
    }

    @SuppressWarnings("unchecked")
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onSendPacket(PacketEvent.Send event) {
        // Move packet event
        if (toggleMovePacket.get() && event.packet instanceof PlayerMoveC2SPacket packet) {
            movePacketsPos.add(new Vec3d(packet.getX(mc.player.getX()), packet.getY(mc.player.getY()), packet.getZ(mc.player.getZ())));
        }

        // Packet Debug
        if (togglePacket.get() && packetC2S.get().contains(event.packet.getClass()))
            msg(PacketUtils.getName((Class<? extends Packet<?>>) event.packet.getClass()) + " " + String.valueOf(event.packet));
    }
    @SuppressWarnings("unchecked")
    @EventHandler(priority = EventPriority.HIGHEST + 2)
    private void onReceivePacketHighest(PacketEvent.Receive event) {
        // Packet Debug
        if (togglePacket.get() && packetS2C.get().contains(event.packet.getClass()))
            msg(PacketUtils.getName((Class<? extends Packet<?>>) event.packet.getClass()) + " " + String.valueOf(event.packet));
    }
    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        // PJL
        if (
            event.packet instanceof PlayerListS2CPacket packet
            && togglePJL.get()
            && packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)
            && (pjlMode.get() == PJLMode.Both || pjlMode.get() == PJLMode.Joins)
        ) pjlJoins(packet);
        if (
            event.packet instanceof PlayerRemoveS2CPacket packet
            && togglePJL.get()
            && (pjlMode.get() == PJLMode.Both || pjlMode.get() == PJLMode.Leaves)
        ) pjlLeaves(packet);

        // Get combot entity
        if (
            event.packet instanceof PlayerInteractEntityC2SPacket packet
            && com.omsent.addon.Utils.EntityUtils.isAttack(packet)
        ) {
            combotEntity = com.omsent.addon.Utils.EntityUtils.getEntity(packet);
        }
    }
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (toggleMovePacket.get()) drawMovePacket(event);
        if (toggleCombot.get()) drawCombotPlayer(event);
    }
    @EventHandler(priority = EventPriority.LOWEST)
    private void onRender2D(Render2DEvent event) {
        if (toggleHud.get()) drawHud(event);
        if (toggleNotify.get()) drawNotify(event);
    }

    // Death Of
    private void sendDeathOf(PlayerEntity player, LivingEntity killer) {
        String deathMsg = String.format("%s died by %s", EntityUtils.getName(player), killer == null ? "NullPoint" : EntityUtils.getName(killer));
        if (deathOfNotify.get()) addNotify("Death message", deathMsg, null, new Color(255, 30, 30));
        msg(deathMsg);
    }
    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {
        if (!(event.entity instanceof PlayerEntity entity)) return;
        if (entity instanceof LivingEntity e) {
            if (!e.isDead()) return;
        } else return;
        if (entity.isAlive()) return;

        if (entity.getRecentDamageSource() == null) {
            sendDeathOf(entity, null);
            return;
        }
        DamageSource source = entity.getRecentDamageSource();
        if (!(source.getAttacker() instanceof LivingEntity killer)) return;
        sendDeathOf(entity, killer);
    }

    private void drawMovePacket(Render3DEvent event) {
        List<Vec3d> items = new ArrayList<>(movePacketsPos);
        List<Vec3d> lags = new ArrayList<>(lagPacketsPos);
        if (!items.isEmpty()) for (Vec3d pos : items) {
            if (pos == null) continue;
            event.renderer.box(pos.x - 0.3, pos.y, pos.z - 0.3,
                    pos.x + 0.3, pos.y + 1.8, pos.z + 0.3,
                    movePacketColor.get(), movePacketColor.get(), ShapeMode.Lines, 0);
        }
        if (!lags.isEmpty()) for (Vec3d pos : lags) {
            if (pos == null) continue;
            event.renderer.box(pos.x - 0.3, pos.y, pos.z - 0.3,
                    pos.x + 0.3, pos.y + 1.8, pos.z + 0.3,
                    lagPacketColor.get(), lagPacketColor.get(), ShapeMode.Lines, 0);
        }
    }

    private void drawCombotPlayer(Render3DEvent event) {
        if (combotEntity == null) return;
        if (!combotEntity.isAlive() || (combotEntity instanceof LivingEntity entity && entity.isDead())) return;
        event.renderer.box(
            combotEntity.getBoundingBox(),
            combotEntityColor.get(),
            combotEntityColor.get(),
            ShapeMode.Both,
            0
        );
    }
    private void drawHud(Render2DEvent event) {
        if (mc.player == null || mc.options.hudHidden) return;
        double w = (double) Utils.getWindowWidth();
        double h = (double) Utils.getWindowHeight();
        double cX = Math.ceil(w / 2);
        double cY = Math.ceil(h / 2);
        double x = (double) (cX - 182);
        double y = (double) (h - 44);
        boolean offhandEmpty = !InvUtils.testInOffHand(Items.AIR);
        HudRenderer renderer = HudRenderer.INSTANCE;
        int slot = mc.player.getInventory().selectedSlot;
        Color hotbarColor = hudHotbarColor.get();
        Color slotColor = hudHotbarSlotColor.get();

        renderer.begin(event.drawContext);

        // Render background in hotbar
        renderer.quad(x, y, 360, 40, hotbarColor);
        if (offhandEmpty) {
            renderer.quad(x - 58, y, 40, 40, hotbarColor);
        }
        // Render slot in hotbar
        hudSlotTransi.update();
        hudSlotTransi = Transition.create(
            hudSlotTransi,
            x + 360 / 9 * slot,
            p -> renderer.quad(p, y, 40, 40, slotColor)
        );
        // Render item on background
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            final int a = i;
            renderer.post(() -> {
                renderer.item(
                    stack,
                    (int) (x + 6 + 360 / 9 * a), (int) y + 4,
                    2f,
                    true
                );
            });
        }
        // Render offhand item
        renderer.post(() -> {if (offhandEmpty) {
            renderer.item(
                mc.player.getOffHandStack(),
                (int) x + 6 - 58, (int) y + 4,
                2f,
                true
            );
        }});
        // Fix bug
        renderer.post(() -> renderer.item(
            fixBugStack,
            -Utils.getWindowWidth(), -Utils.getWindowHeight(),
            0f,
            true
        ));
        // Render EXP
        renderer.quad(x, y - 10, 360, 6, hotbarColor); // Is EXP background
        hudXPTransi.update();
        hudXPTransi = Transition.create(
            hudXPTransi,
            mc.player.experienceProgress * 360,
            p -> {
                renderer.quad(x, y - 10, p, 6, new Color(0, 255, 50));
            }
        );
        renderer.text(
            String.valueOf(mc.player.experienceLevel),
            x + 180 - renderer.textWidth(String.valueOf(mc.player.experienceLevel), true, 1) / 2, y - 27,
            new Color(0,255,50),
            true,
            1
        ); // Draw EXP level

        // Render health
        renderer.quad(
            x, y - 26,
            127, 12,
            hudHotbarColor.get()
        );
        hudHealthTransi.update();
        hudHealthTransi = Transition.create(
            hudHealthTransi,
            127 * mc.player.getHealth() / mc.player.getMaxHealth(),
            p -> {
                renderer.quad(
                    x, y - 26,
                    p, 12,
                    new Color(255, 50, 50)
                );
            }
        );
        if (mc.player.getAbsorptionAmount() > 0.0) renderer.text(
            "+" + String.valueOf(mc.player.getAbsorptionAmount()),
            x, y - 20 - renderer.textHeight(true, 1) / 2,
            new Color(255, 255, 100),
            true,
            1
        );
        // Render hunger
        renderer.quad(
            x + 360, y - 26,
            -127, 12,
            hudHotbarColor.get()
        );
        hudHungerTransi.update();
        hudHungerTransi = Transition.create(
            hudHungerTransi,
            -127 * mc.player.getHungerManager().getFoodLevel() / 20,
            p -> renderer.quad(
                x + 360, y - 26,
                p, 12,
                new Color(255, 100, 0)
            )
        );
        hudSatTransi.update();
        hudSatTransi = Transition.create(
            hudSatTransi,
            -127 * mc.player.getHungerManager().getSaturationLevel() / 20,
            p -> renderer.quad(
                x + 360, y - 26,
                p, 12,
                new Color(255, 255, 0)
            )
        );
        // Render air
        if (mc.player.getAir() < mc.player.getMaxAir()) {
            renderer.quad(
                x + 360, y - 26 - 16,
                -127, 12,
                hudHotbarColor.get()
            );
            float p = -127 * mc.player.getAir() / mc.player.getMaxAir();
            renderer.quad(
                x + 360, y - 26 - 16,
                p > 0 ? 0 : p, 12,
                new Color(0,100,255)
            );
        }
        // Render armor
        if (mc.player.getArmor() > 0) {
            renderer.quad(
                x, y - 26 - 16,
                127, 12,
                hudHotbarColor.get()
            );
        }
        hudArmorTransi.update();
        hudArmorTransi = Transition.create(
            hudArmorTransi,
            127 * mc.player.getArmor() / 20,
            p -> {
                renderer.quad(
                    x, y - 26 - 16,
                    p, 12,
                    new Color(220, 220, 220)
                );
            }
        );

        renderer.end();
    }
    private void drawNotify(Render2DEvent event) {
        long nowTime = System.currentTimeMillis();

        notifies.removeIf(notify -> {
            if (notifyPosition.get().getX() < Utils.getWindowWidth() / 2 && notify.x < notifyPosition.get().getX() - notifyWidth.get() / 2 - 10)
                return true;
            else if (notifyPosition.get().getX() >= Utils.getWindowWidth() / 2 && notify.x > notifyPosition.get().getX() + notifyWidth.get() / 2 + 10)
                return true;

            return false;
        });

        List<Notify> notifiesCopy = new ArrayList<>(notifies);

        for (int i = 0; i < notifiesCopy.size(); i++) {
            Notify notify = notifiesCopy.get(i);

            // Notify in
            double x = notifyPosition.get().getX() < Utils.getWindowWidth() / 2 ?
                notifyPosition.get().getX() + notifyWidth.get() / 2 + 10 :
                notifyPosition.get().getX() - notifyWidth.get() / 2 - 10;
            double y = notifyUp() ?
                (notifyPosition.get().getY() + notifyHeight.get() / 2) + i * notifyHeight.get() + 10 :
                (notifyPosition.get().getY() - notifyHeight.get() / 2) - (notifiesCopy.size() - 1 - i) * notifyHeight.get() - 10;

            // Remove
            if (notify.time + notify.stayTime * 1000 < nowTime) {
                if (notifyPosition.get().getX() < Utils.getWindowWidth() / 2) {
                    notify.x += (double) (notify.time + notify.stayTime * 1000 - nowTime) / (Math.pow(notifyTransiSpeed.get(), 2));
                } else {
                    notify.x -= (double) (notify.time + notify.stayTime * 1000 - nowTime) / (Math.pow(notifyTransiSpeed.get(), 2));
                }
            } else notify.x += (x - notify.x) / notifyTransiSpeed.get();

            notify.y += (y - notify.y) / notifyTransiSpeed.get();
            notify.drawNotify(event);
        }
    }
    // Notify in up
    private boolean notifyUp() {
        return notifyPosition.get() == NotifyPosition.LeftTop || notifyPosition.get() == NotifyPosition.RightTop;
    }
    // Add notify
    public void addNotify(Notify notify) {
        notifies.add(notify);
    }
    public void addNotify(String title, String message, Color bg, Color fg) {
        addNotify(new Notify(
            title,
            message,
            bg == null ? new Color(0, 0, 0, 178) : bg,
            fg == null ? new Color(255, 255, 255) : fg
        ));
    }
    public void addNotify(String title, String message) {
        addNotify(title, message, null, null);
    }
    // Add module notify
    public void addModuleNotify(String name, boolean isActive) {
        addNotify(new Notify(
            isActive ? "On" : "Off",
            name,
            isActive ? notifyActiveColor.get() : notifyDeactiveColor.get(),
            isActive ? notifyActiveTextColor.get() : notifyDeactiveTextColor.get()
        ));
    }

    private void pjlJoins(PlayerListS2CPacket packet) {
        for (PlayerListS2CPacket.Entry entry : packet.getPlayerAdditionEntries()) {
            if (entry.profile() == null) continue;

            String name = entry.profile().getName();

            msg(Text.literal(
                Formatting.RED + "[>"
                + Formatting.GREEN + "+"
                + Formatting.RED + "<]"
                + name
            ), name);
        }
    }
    private void pjlLeaves(PlayerRemoveS2CPacket packet) {
        if (!TPUtils.allowSendP()) return;

        for (UUID id : packet.profileIds()) {
            PlayerListEntry toRemove = mc.getNetworkHandler().getPlayerListEntry(id);
            if (toRemove == null) continue;
            String name = toRemove.getProfile().getName();

            msg(Text.literal(
                Formatting.GRAY + "[---]"
                + name
            ),name);
        }
    }

    @Override
    public void onDeactivate() {
        movePacketsPos.clear();
        lagPacketsPos.clear();
        combotEntity = null;
    }

    public enum PJLMode {
        Both,
        Joins,
        Leaves
    }

    public enum NotifyPosition {
        LeftTop,
        LeftBottom,
        RightTop,
        RightBottom;

        public double getX() { return this == NotifyPosition.LeftTop || this == NotifyPosition.LeftBottom ? 0.0 : Utils.getWindowWidth(); }
        public double getY() { return this == NotifyPosition.LeftTop || this == NotifyPosition.RightTop ? 0.0 : Utils.getWindowHeight(); }
    }

    private double getXFrom() {
        return notifyPosition.get().getX() < Utils.getWindowWidth() / 2 ?
            notifyPosition.get().getX() - notifyWidth.get() / 2 :
            notifyPosition.get().getX() + notifyWidth.get() / 2;
    }
    private double getYFrom() {
        return notifyUp() ?
            notifyPosition.get().getY() - notifyHeight.get() / 2 :
            notifyPosition.get().getY() + notifyHeight.get() / 2;
    }
    public class Notify {
        public final String title;
        public final String message;
        public final Color bg;
        public final Color fg;
        public final long time;
        public final int stayTime;
        public double x = getXFrom();
        public double y = getYFrom();

        public Notify(String title, String message, Color bg, Color fg) {
            this.title = title;
            this.message = message;
            this.bg = bg;
            this.fg = fg;
            this.time = System.currentTimeMillis();
            this.stayTime = notifyStayTime.get();
        }
        public Notify(Notify notify) {
            this.title = notify.title;
            this.message = notify.message;
            this.bg = notify.bg;
            this.fg = notify.fg;
            this.time = notify.time;
            this.stayTime = notify.stayTime;
            this.x = notify.x;
            this.y = notify.y;
        }
        public void drawNotify(Render2DEvent event) {
            HudRenderer renderer = HudRenderer.INSTANCE;
            double width = notifyWidth.get();
            double height = notifyHeight.get();
            double left = this.x - width / 2;
            double top = this.y - height / 2;
            double currentWidth = (1.0 - (System.currentTimeMillis() - this.time) / (this.stayTime * 1000.0)) * width;
            renderer.begin(event.drawContext);

            renderer.quad(
                left, top, width, height,
                this.bg
            );

            // Process
            renderer.quad(
                left, top + height - 3,
                width,
                3,
                new Color(0, 0, 0, 178)
            );
            renderer.quad(
                left, top + height - 3,
                Math.max(Math.min(width, currentWidth), 0.0),
                3,
                new Color(255, 255, 255, 255)
            );

            renderer.text(
                this.title,
                left, top,
                this.fg,
                notifyTextShadow.get(),
                1.3
            );
            renderer.text(
                this.message,
                left, top + height - renderer.textHeight(notifyTextShadow.get(), 1.3),
                this.fg,
                notifyTextShadow.get(),
                0.8
            );
            renderer.end();
        }

        public String toString() {
            return String.format("Notify[title=%s, message=%s]", this.title, this.message);
        }
    }
}
