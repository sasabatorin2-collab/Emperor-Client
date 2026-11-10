package dev.nova.modules;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.NumberSetting;

public class Flight extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", 0.10, 0.02, 0.50, 0.01));

    public Flight() {
        super("Flight", "Lets you fly like in creative mode.", Category.MOVEMENT);
    }

    @Override
    public String getInfo() {
        return String.format(java.util.Locale.ROOT, "%.2f", speed.d());
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        mc.player.getAbilities().allowFlying = true;
        mc.player.getAbilities().setFlySpeed(speed.f());
    }

    @Override
    protected void onDisable() {
        if (mc.player == null) return;
        mc.player.getAbilities().setFlySpeed(0.05f);
        if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().allowFlying = false;
        }
    }
}
