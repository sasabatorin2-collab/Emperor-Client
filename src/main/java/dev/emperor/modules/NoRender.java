package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.setting.BoolSetting;

public class NoRender extends Module {
    private final BoolSetting hurtCam = add(new BoolSetting("Hurt camera", true));
    private final BoolSetting fire = add(new BoolSetting("Fire overlay", true));
    private final BoolSetting underwater = add(new BoolSetting("Underwater overlay", true));
    private final BoolSetting pumpkin = add(new BoolSetting("Pumpkin", true));
    private final BoolSetting vignette = add(new BoolSetting("Vignette", false));
    private final BoolSetting totem = add(new BoolSetting("Totem animation", false));

    public NoRender() {
        super("NoRender", "Hides distracting overlays and effects.", Category.RENDER);
    }

    private static boolean on(java.util.function.Function<NoRender, BoolSetting> f) {
        NoRender n = ModuleManager.get(NoRender.class);
        return n != null && n.isEnabled() && f.apply(n).get();
    }

    public static boolean hurtCam() {
        return on(n -> n.hurtCam);
    }

    public static boolean fire() {
        return on(n -> n.fire);
    }

    public static boolean underwater() {
        return on(n -> n.underwater);
    }

    public static boolean pumpkin() {
        return on(n -> n.pumpkin);
    }

    public static boolean vignette() {
        return on(n -> n.vignette);
    }

    public static boolean totem() {
        return on(n -> n.totem);
    }
}
