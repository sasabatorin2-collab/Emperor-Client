package dev.nova.modules;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.BoolSetting;

public class HudModule extends Module {
    public final BoolSetting watermark = add(new BoolSetting("Watermark", true));
    public final BoolSetting fps = add(new BoolSetting("FPS", true));
    public final BoolSetting coords = add(new BoolSetting("Coordinates", true));
    public final BoolSetting arrayList = add(new BoolSetting("Module list", true));
    public final BoolSetting rainbow = add(new BoolSetting("Rainbow", true));

    public HudModule() {
        super("HUD", "Overlay with client info and active modules.", Category.CLIENT);
    }
}
