package dev.emperor.hud;

import dev.emperor.module.ModuleManager;
import dev.emperor.modules.ClickGuiModule;

import java.awt.Color;

/** Two-colour animated gradient themes shared by the ClickGUI, HUD and render modules. */
public final class Theme {
    public static final String[] NAMES = {
            "Emperor", "Amethyst", "Crimson", "Ocean", "Aurora", "Sakura", "Mint", "Sunset", "Midnight", "Rainbow"
    };

    private Theme() {
    }

    private static ClickGuiModule cfg() {
        return ModuleManager.get(ClickGuiModule.class);
    }

    private static String mode() {
        ClickGuiModule g = cfg();
        return g == null ? "Emperor" : g.theme.get();
    }

    private static int[] pair() {
        return switch (mode()) {
            case "Amethyst" -> new int[]{0xFFC9A6FF, 0xFF6A3DFF};
            case "Crimson" -> new int[]{0xFFFF6F8E, 0xFFB3122E};
            case "Ocean" -> new int[]{0xFF4FE3FF, 0xFF3A5BFF};
            case "Aurora" -> new int[]{0xFF4CFFB0, 0xFF8A5CFF};
            case "Sakura" -> new int[]{0xFFFFC2E2, 0xFFFF4FA3};
            case "Mint" -> new int[]{0xFFC4FFE0, 0xFF1FCF83};
            case "Sunset" -> new int[]{0xFFFFC94C, 0xFFFF4C7A};
            case "Midnight" -> new int[]{0xFF9FB4FF, 0xFF3B2D8F};
            default -> new int[]{0xFFFFD36E, 0xFFB45CFF}; // Emperor: imperial gold -> royal purple
        };
    }

    private static double speed() {
        ClickGuiModule g = cfg();
        return g == null ? 1.0 : g.gradientSpeed.d();
    }

    private static boolean rainbow() {
        return "Rainbow".equals(mode());
    }

    public static int primary() {
        return rainbow() ? accent(0) : pair()[0];
    }

    public static int secondary() {
        return rainbow() ? accent(12) : pair()[1];
    }

    public static int main() {
        return accent(0);
    }

    /** Animated colour that flows between the two theme colours; index offsets the phase. */
    public static int accent(int index) {
        double t = System.currentTimeMillis() / 1000.0 * speed();
        if (rainbow()) {
            float hue = (float) ((t * 0.25 + index * 0.035) % 1.0);
            return Color.HSBtoRGB(hue, 0.55f, 1f) | 0xFF000000;
        }
        int[] p = pair();
        float f = (float) (0.5 + 0.5 * Math.sin(t * 2.0 - index * 0.35));
        return lerp(p[0], p[1], f);
    }

    /** Gradient sampled along a strip (0..1) with a slow shimmer, used for headers and bars. */
    public static int gradient(float pos) {
        double t = System.currentTimeMillis() / 1000.0 * speed();
        if (rainbow()) {
            float hue = (float) ((t * 0.25 + pos * 0.35) % 1.0);
            return Color.HSBtoRGB(hue, 0.55f, 1f) | 0xFF000000;
        }
        int[] p = pair();
        float f = (float) (0.5 + 0.5 * Math.sin(t * 1.6 + pos * Math.PI));
        return lerp(p[0], p[1], f);
    }

    public static int radius() {
        ClickGuiModule g = cfg();
        return g == null ? 6 : g.rounding.i();
    }

    public static float opacity() {
        ClickGuiModule g = cfg();
        return g == null ? 0.9f : g.opacity.f();
    }

    /** Replaces the alpha channel of an ARGB colour. */
    public static int alpha(int argb, int a) {
        return (Math.max(0, Math.min(255, a)) << 24) | (argb & 0xFFFFFF);
    }

    /** Multiplies the existing alpha of an ARGB colour. */
    public static int fade(int argb, float mul) {
        int a = (argb >>> 24) & 0xFF;
        return alpha(argb, Math.round(a * Math.max(0f, Math.min(1f, mul))));
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24)
                | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8)
                | (int) (ab + (bb - ab) * t);
    }
}
