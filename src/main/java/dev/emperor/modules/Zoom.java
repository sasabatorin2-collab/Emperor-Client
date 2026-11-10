package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;

/** Smooth zoom; scroll while zoomed to change the level. */
public class Zoom extends Module {
    private final NumberSetting factor = add(new NumberSetting("Factor", 4, 1.5, 15, 0.5));
    private final BoolSetting hold = add(new BoolSetting("Hold key", true));
    private final BoolSetting smooth = add(new BoolSetting("Smooth", true));
    private final BoolSetting sensitivity = add(new BoolSetting("Lower sensitivity", true));

    private static float current = 1f;
    private static long lastNs = System.nanoTime();
    private double scroll = 1.0;

    public Zoom() {
        super("Zoom", "Smooth OptiFine-style zoom (bind a key, scroll to adjust).", Category.RENDER);
    }

    @Override
    public boolean saveEnabled() {
        return false;
    }

    @Override
    public boolean showNotifications() {
        return false;
    }

    @Override
    protected void onEnable() {
        scroll = 1.0;
    }

    @Override
    public void onTick() {
        // With "Hold key" the module only stays on while its bind is held down.
        if (hold.get() && getKey() >= 0 && !holdMode && !dev.emperor.util.Keys.isDown(getKey())) setEnabled(false);
    }

    private static Zoom inst() {
        return ModuleManager.get(Zoom.class);
    }

    private float target() {
        return isEnabled() ? (float) Math.min(50, factor.d() * scroll) : 1f;
    }

    public static float apply(float fov) {
        Zoom z = inst();
        if (z == null) return fov;
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
        float t = z.target();
        current = z.smooth.get() ? current + (t - current) * (1f - (float) Math.exp(-dt * 12f)) : t;
        if (Math.abs(current - 1f) < 0.002f && t == 1f) current = 1f;
        return fov / current;
    }

    public static double sensitivity() {
        Zoom z = inst();
        if (z == null || !z.sensitivity.get() || current <= 1.001f) return 1.0;
        return 1.0 / current;
    }

    public static boolean onScroll(double amount) {
        Zoom z = inst();
        if (z == null || !z.isEnabled() || MinecraftClient.getInstance().currentScreen != null || amount == 0) return false;
        z.scroll = Math.max(0.25, Math.min(4.0, z.scroll * (amount > 0 ? 1.15 : 1 / 1.15)));
        return true;
    }
}
