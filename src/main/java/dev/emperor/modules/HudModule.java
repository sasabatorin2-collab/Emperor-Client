package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;

public class HudModule extends Module {
    public final BoolSetting watermark = add(new BoolSetting("Watermark", true));
    public final BoolSetting arrayList = add(new BoolSetting("Module list", true));
    public final BoolSetting info = add(new BoolSetting("Info panel", true));
    public final BoolSetting armor = add(new BoolSetting("Armor", true));
    public final BoolSetting targetHud = add(new BoolSetting("Target HUD", true));
    public final BoolSetting notifications = add(new BoolSetting("Notifications", true));

    public HudModule() {
        super("HUD", "Overlay with watermark, module list, info, armor and target.", Category.CLIENT);
    }

    @Override
    public boolean showNotifications() {
        return false;
    }
}
