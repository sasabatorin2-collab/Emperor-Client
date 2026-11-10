package dev.emperor.modules;

import dev.emperor.hud.Hud;
import dev.emperor.hud.HudElement;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.NumberSetting;

public class HudModule extends Module {
    public final BoolSetting notifications;
    public final NumberSetting radarRange;

    public HudModule() {
        super("HUD", "Draggable widgets: open the HUD Editor from the menu dock.", Category.CLIENT);
        for (HudElement e : Hud.ELEMENTS) add(e.visible);
        notifications = add(new BoolSetting("Notifications", true));
        radarRange = add(new NumberSetting("Radar range", 48, 16, 128, 8));
    }

    @Override
    public boolean showNotifications() {
        return false;
    }
}
