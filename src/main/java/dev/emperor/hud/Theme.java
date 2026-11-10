package dev.emperor.hud;

import dev.emperor.module.ModuleManager;
import dev.emperor.modules.ClickGuiModule;

import java.awt.Color;

public final class Theme {
    private Theme() {
    }

    private static ClickGuiModule cfg() {
        return ModuleManager.get(ClickGuiModule.class);
    }

    public static int main() {
        return accent(0);
    }

    public static int accent(int index) {
        ClickGuiModule g = cfg();
        String mode = g == null ? "Purple" : g.theme.get();
        return switch (mode) {
            case "Blue" -> 0xFF4C9BFF;
            case "Red" -> 0xFFFF5C6C;
            case "Green" -> 0xFF4CE08A;
            case "Orange" -> 0xFFFFA14C;
            case "Rainbow" -> Color.HSBtoRGB(((System.currentTimeMillis() / 20 + index * 14L) % 360) / 360f, 0.55f, 1f);
            default -> 0xFF7C5CFF;
        };
    }

    public static int radius() {
        ClickGuiModule g = cfg();
        return g == null ? 5 : g.rounding.i();
    }

    public static float opacity() {
        ClickGuiModule g = cfg();
        return g == null ? 0.9f : g.opacity.f();
    }

    /** Replaces the alpha channel of an ARGB colour. */
    public static int alpha(int argb, int a) {
        return (Math.max(0, Math.min(255, a)) << 24) | (argb & 0xFFFFFF);
    }
}
