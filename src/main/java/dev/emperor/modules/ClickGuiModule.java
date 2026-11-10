package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.ModeSetting;
import dev.emperor.setting.NumberSetting;

/** Holds the look-and-feel settings for the ClickGUI and HUD. */
public class ClickGuiModule extends Module {
    public final ModeSetting theme = add(new ModeSetting("Theme", "Purple", "Purple", "Blue", "Red", "Green", "Orange", "Rainbow"));
    public final NumberSetting rounding = add(new NumberSetting("Rounding", 5, 0, 8, 1));
    public final NumberSetting opacity = add(new NumberSetting("Opacity", 0.90, 0.40, 1.00, 0.05));

    public ClickGuiModule() {
        super("GUI", "Theme, corner rounding and opacity of the menu.", Category.CLIENT);
    }
}
