package dev.emperor.modules;

import dev.emperor.hud.Theme;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.ColorSetting;
import dev.emperor.setting.ModeSetting;
import dev.emperor.setting.NumberSetting;

/** Holds the look-and-feel settings for the ClickGUI and HUD. */
public class ClickGuiModule extends Module {
    public final ModeSetting theme = add(new ModeSetting("Theme", "Emperor", Theme.NAMES));
    public final ColorSetting customA = add(new ColorSetting("Colour 1", 0xFFFFD36E));
    public final ColorSetting customB = add(new ColorSetting("Colour 2", 0xFFB45CFF));
    public final NumberSetting gradientSpeed = add(new NumberSetting("Gradient speed", 1.0, 0.0, 3.0, 0.1));
    public final NumberSetting rounding = add(new NumberSetting("Rounding", 6, 0, 10, 1));
    public final NumberSetting opacity = add(new NumberSetting("Opacity", 0.90, 0.40, 1.00, 0.05));
    public final BoolSetting blur = add(new BoolSetting("Blur", true));
    public final BoolSetting particles = add(new BoolSetting("Particles", true));
    public final BoolSetting sounds = add(new BoolSetting("Click sounds", true));

    public ClickGuiModule() {
        super("GUI", "Theme, animations and look of the menu.", Category.CLIENT);
    }

    @Override
    public boolean showNotifications() {
        return false;
    }
}
