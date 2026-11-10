package dev.emperor.hud;

import com.google.gson.JsonObject;
import dev.emperor.gui.HudEditorScreen;
import dev.emperor.hud.elements.*;
import dev.emperor.module.ModuleManager;
import dev.emperor.modules.HudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;

import java.util.List;

public final class Hud {
    public static final WatermarkElement WATERMARK = new WatermarkElement();
    public static final ModuleListElement MODULES = new ModuleListElement();
    public static final InfoElement INFO = new InfoElement();
    public static final ArmorElement ARMOR = new ArmorElement();
    public static final TargetHudElement TARGET = new TargetHudElement();
    public static final RadarElement RADAR = new RadarElement();
    public static final CompassElement COMPASS = new CompassElement();
    public static final StatsElement STATS = new StatsElement();
    public static final KeystrokesElement KEYSTROKES = new KeystrokesElement();
    public static final PotionsElement POTIONS = new PotionsElement();
    public static final PerformanceElement PERFORMANCE = new PerformanceElement();
    public static final InventoryElement INVENTORY = new InventoryElement();

    public static final List<HudElement> ELEMENTS = List.of(
            WATERMARK, MODULES, INFO, ARMOR, TARGET, RADAR, COMPASS, STATS, KEYSTROKES, POTIONS, PERFORMANCE, INVENTORY);

    private static long lastFrame = System.nanoTime();

    private Hud() {
    }

    /** Remembers the entity you are fighting so the Target HUD can show it. */
    public static void setTarget(LivingEntity e) {
        TargetHudElement.setTarget(e);
    }

    private static boolean enabled() {
        HudModule h = ModuleManager.get(HudModule.class);
        return h != null && h.isEnabled();
    }

    /** Our potion widget replaces the vanilla effect icons. */
    public static boolean hideVanillaEffects() {
        return enabled() && POTIONS.visible.get();
    }

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof HudEditorScreen) return;
        if (!enabled() || mc.options.hudHidden || mc.player == null) return;
        renderElements(ctx, false);
    }

    /** Draws every widget; preview = editor mode (all widgets, with placeholder data). */
    public static void renderElements(DrawContext ctx, boolean preview) {
        long nowNs = System.nanoTime();
        float dt = Math.min(0.1f, (nowNs - lastFrame) / 1e9f);
        lastFrame = nowNs;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        for (HudElement e : ELEMENTS) {
            if (!preview && (!e.visible.get() || !e.hasContent())) continue;
            e.layout(mc.textRenderer, sw, sh, preview);
            e.render(ctx, mc.textRenderer, dt, preview);
        }
    }

    public static JsonObject save() {
        JsonObject o = new JsonObject();
        for (HudElement e : ELEMENTS) o.add(e.name, e.save());
        return o;
    }

    public static void load(JsonObject o) {
        for (HudElement e : ELEMENTS) {
            if (o.has(e.name) && o.get(e.name).isJsonObject()) e.load(o.getAsJsonObject(e.name));
        }
    }
}
